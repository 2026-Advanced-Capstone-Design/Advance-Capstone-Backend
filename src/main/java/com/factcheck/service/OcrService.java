package com.factcheck.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class OcrService {

    @Value("${ocr.tessdata-path}")
    private String tessdataPath;

    @Value("${ocr.language}")
    private String ocrLanguage;

    @Value("${clova.ocr.api-url}")
    private String clovaApiUrl;

    @Value("${clova.ocr.secret-key}")
    private String clovaSecretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final int MIN_QUALITY_LENGTH = 50;
    private static final double MIN_CONFIDENCE = 0.7;

    public String extractText(MultipartFile imageFile) throws IOException {
        File tempFile = File.createTempFile("ocr_", "_" + imageFile.getOriginalFilename());
        imageFile.transferTo(tempFile);
        try {
            return extractText(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    public String extractText(File imageFile) throws IOException {
        File pngFile = convertToPng(imageFile);
        File processFile = pngFile != null ? pngFile : imageFile;

        try {
            String clovaResult = runClovaOcr(processFile);
            log.info("Clova OCR 결과 길이: {}", clovaResult.length());

            if (!clovaResult.isBlank()) {
                return clovaResult;
            }

            log.info("Clova OCR 결과 없음, Tesseract fallback 시도");
            return runTesseract(processFile);

        } finally {
            if (pngFile != null) pngFile.delete();
        }
    }

    private File convertToPng(File imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                log.warn("이미지 포맷 변환 불가 (HEIC 등 미지원 포맷): {}", imageFile.getName());
                return null;
            }
            File pngFile = File.createTempFile("ocr_converted_", ".png");
            ImageIO.write(image, "PNG", pngFile);
            return pngFile;
        } catch (IOException e) {
            log.warn("PNG 변환 실패: {}", e.getMessage());
            return null;
        }
    }

    private String runTesseract(File imageFile) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(ocrLanguage);
        tesseract.setPageSegMode(3);

        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                log.warn("Tesseract: 이미지 읽기 실패");
                return "";
            }
            return tesseract.doOCR(image);
        } catch (TesseractException | IOException e) {
            log.warn("Tesseract OCR 실패: {}", e.getMessage());
            return "";
        }
    }

    private String runClovaOcr(File imageFile) {
        try {
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String requestBody = """
                    {
                      "version": "V2",
                      "requestId": "ocr-request",
                      "timestamp": %d,
                      "images": [
                        {
                          "format": "png",
                          "name": "news_image",
                          "data": "%s"
                        }
                      ]
                    }
                    """.formatted(System.currentTimeMillis(), base64Image);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", clovaSecretKey);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(clovaApiUrl, request, Map.class);

            return parseClovaResponse(response.getBody());

        } catch (Exception e) {
            log.warn("Clova OCR 실패: {}", e.getMessage());
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private String parseClovaResponse(Map<String, Object> body) {
        if (body == null) return "";
        try {
            var images = (java.util.List<Map<String, Object>>) body.get("images");
            if (images == null || images.isEmpty()) return "";

            var fields = (java.util.List<Map<String, Object>>) images.get(0).get("fields");
            if (fields == null) return "";

            StringBuilder sb = new StringBuilder();
            int total = 0, filtered = 0;
            for (Map<String, Object> field : fields) {
                String text = (String) field.get("inferText");
                if (text == null) continue;
                total++;
                double confidence = ((Number) field.getOrDefault("inferConfidence", 0.0)).doubleValue();
                if (confidence >= MIN_CONFIDENCE) {
                    sb.append(text).append(" ");
                } else {
                    filtered++;
                }
            }
            log.info("Clova OCR confidence 필터링: 전체 {}개 중 {}개 제거 (threshold={})", total, filtered, MIN_CONFIDENCE);
            return sb.toString().trim();

        } catch (Exception e) {
            log.warn("Clova 응답 파싱 실패: {}", e.getMessage());
            return "";
        }
    }
}

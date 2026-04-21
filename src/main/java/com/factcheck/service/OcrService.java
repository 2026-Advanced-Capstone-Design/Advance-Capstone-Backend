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

    public String extractText(MultipartFile imageFile) throws IOException {
        File tempFile = File.createTempFile("ocr_", "_" + imageFile.getOriginalFilename());
        imageFile.transferTo(tempFile);

        try {
            String tesseractResult = runTesseract(tempFile);
            log.info("Tesseract 결과 길이: {}", tesseractResult.length());

            if (tesseractResult.trim().length() < MIN_QUALITY_LENGTH) {
                log.info("Tesseract 품질 미흡, Clova OCR fallback 시도");
                String clovaResult = runClovaOcr(tempFile);
                if (!clovaResult.isBlank()) {
                    return clovaResult;
                }
            }

            return tesseractResult;

        } finally {
            tempFile.delete();
        }
    }

    private String runTesseract(File imageFile) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(ocrLanguage);
        tesseract.setPageSegMode(3);

        try {
            BufferedImage image = ImageIO.read(imageFile);
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
                          "format": "jpg",
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
            for (Map<String, Object> field : fields) {
                String text = (String) field.get("inferText");
                if (text != null) sb.append(text).append(" ");
            }
            return sb.toString().trim();

        } catch (Exception e) {
            log.warn("Clova 응답 파싱 실패: {}", e.getMessage());
            return "";
        }
    }
}

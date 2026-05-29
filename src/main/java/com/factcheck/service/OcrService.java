package com.factcheck.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Slf4j
@Service
public class OcrService {

    @Value("${ocr.tessdata-path}")
    private String tessdataPath;

    @Value("${ocr.language}")
    private String ocrLanguage;

    @Value("${OPENAI_API_KEY:}")
    private String openaiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MIN_QUALITY_LENGTH = 50;
    private static final double MIN_KOREAN_RATIO = 0.3;
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String VISION_MODEL = "gpt-4o-mini";

    public String extractText(MultipartFile imageFile) throws IOException {
        File tempFile = File.createTempFile("ocr_", "_" + imageFile.getOriginalFilename());
        imageFile.transferTo(tempFile);

        try {
            String tesseractResult = runTesseract(tempFile);
            log.info("Tesseract 결과 길이: {}", tesseractResult.length());

            // 품질 검증 (길이 + 한글 비율)
            if (needsFallback(tesseractResult)) {
                log.info("Tesseract 품질 미흡 - GPT Vision OCR fallback 시도");
                String visionResult = runGptVisionOcr(tempFile);
                if (!visionResult.isBlank()) {
                    log.info("GPT Vision OCR 성공: {} 자", visionResult.length());
                    return visionResult;
                }
                log.warn("Vision fallback 실패 - Tesseract 결과 반환");
            }

            return tesseractResult;

        } finally {
            tempFile.delete();
        }
    }

    /**
     * Tesseract 결과의 품질을 판단하여 fallback 필요 여부 결정
     * 1) 길이가 너무 짧음 → fallback
     * 2) 한글 비율이 30% 미만 → 한글 기사인데 영문/기호로 깨진 경우 → fallback
     */
    private boolean needsFallback(String text) {
        if (text == null || text.trim().length() < MIN_QUALITY_LENGTH) {
            return true;
        }

        long koreanCount = text.chars()
                .filter(c -> c >= 0xAC00 && c <= 0xD7A3)
                .count();
        double koreanRatio = (double) koreanCount / text.length();
        log.info("한글 비율: {}", String.format("%.2f", koreanRatio));

        return koreanRatio < MIN_KOREAN_RATIO;
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

    /**
     * GPT-4o-mini Vision API를 사용한 OCR
     * Tesseract가 한글 인식에 실패한 경우 fallback으로 호출
     */
    private String runGptVisionOcr(File imageFile) {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            log.warn("OPENAI_API_KEY 미설정 - Vision OCR 스킵");
            return "";
        }

        try {
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = detectMimeType(imageFile);

            String requestBody = String.format("""
                    {
                      "model": "%s",
                      "messages": [{
                        "role": "user",
                        "content": [
                          {
                            "type": "text",
                            "text": "이 뉴스 기사 이미지의 본문 텍스트를 정확히 추출해줘. 한국어 본문만 그대로 반환하고 다른 설명은 하지마. 줄바꿈은 유지해."
                          },
                          {
                            "type": "image_url",
                            "image_url": {
                              "url": "data:%s;base64,%s"
                            }
                          }
                        ]
                      }],
                      "max_tokens": 4000
                    }
                    """, VISION_MODEL, mimeType, base64Image);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_API_URL, request, String.class);

            return parseOpenAiResponse(response.getBody());

        } catch (Exception e) {
            log.warn("GPT Vision OCR 실패: {}", e.getMessage());
            return "";
        }
    }

    private String detectMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private String parseOpenAiResponse(String responseBody) {
        if (responseBody == null) return "";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText("");
                return content.trim();
            }
            return "";
        } catch (Exception e) {
            log.warn("OpenAI 응답 파싱 실패: {}", e.getMessage());
            return "";
        }
    }
}

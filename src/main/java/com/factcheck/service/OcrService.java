package com.factcheck.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OcrService {

    @Value("${ocr.tessdata-path}")
    private String tessdataPath;

    @Value("${ocr.language}")
    private String ocrLanguage;

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    private static final String VISION_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String VISION_MODEL = "gpt-4o-mini";

    private static final double NOISE_RATIO_THRESHOLD = 0.10;
    private static final double KOREAN_RATIO_THRESHOLD = 0.5;
    private static final int MIN_TEXT_LENGTH = 50;

    private final RestTemplate visionRestTemplate;

    /**
     * 재사용하는 단일 Tesseract 인스턴스 (Phase 3).
     * 이전에는 호출마다 {@code new Tesseract()}를 만들어 네이티브 리소스(tessdata 사전,
     * libtesseract 핸들)를 반복 로드했다 — JVM 힙 밖(네이티브) 메모리라 GC가 회수하지 못해
     * OCR 요청이 쌓일수록 프로세스 RSS가 계속 커지는 누수 패턴.
     * Tess4J의 Tesseract는 스레드 안전하지 않으므로 사용 시 동기화한다(ocrExecutor 2~4스레드).
     */
    private Tesseract tesseract;

    public OcrService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.visionRestTemplate = new RestTemplate(factory);
    }

    @jakarta.annotation.PostConstruct
    void initTesseract() {
        tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(ocrLanguage);
        tesseract.setPageSegMode(3);
    }

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
            String tesseractResult = runTesseract(processFile);
            log.info("Tesseract OCR 결과 길이: {}", tesseractResult.length());

            if (needsVisionFallback(tesseractResult)) {
                log.info("Tesseract 품질 미달, Vision fallback 시도");
                String visionResult = runVisionOcr(processFile);
                if (!visionResult.isBlank()) {
                    log.info("Vision OCR 성공, 길이: {}", visionResult.length());
                    return visionResult;
                }
                log.warn("Vision OCR 실패/빈 결과, Tesseract 결과 사용");
            }

            return tesseractResult;
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
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                log.warn("Tesseract: 이미지 읽기 실패");
                return "";
            }
            // Tesseract 인스턴스는 스레드 비안전 → OCR 구간만 직렬화.
            // t2.micro는 vCPU 1이라 OCR(네이티브 CPU 바운드) 병렬화 실익도 없다.
            synchronized (tesseract) {
                return tesseract.doOCR(image);
            }
        } catch (TesseractException | IOException e) {
            log.warn("Tesseract OCR 실패: {}", e.getMessage());
            return "";
        }
    }

    private boolean needsVisionFallback(String text) {
        if (text == null || text.isBlank()) return true;
        if (text.length() < MIN_TEXT_LENGTH) return true;

        String[] tokens = text.split("\\s+");
        int total = 0, noisy = 0;
        for (String token : tokens) {
            if (token.length() < 3) continue;
            total++;
            if (isNoisyToken(token)) noisy++;
        }

        double noiseRatio = total > 0 ? (double) noisy / total : 1.0;
        double koreanRatio = calculateKoreanRatio(text);

        log.info("OCR 품질 검증: 한글={}%, 노이즈={}%, 토큰={}개",
                (int) (koreanRatio * 100), (int) (noiseRatio * 100), total);

        return noiseRatio > NOISE_RATIO_THRESHOLD || koreanRatio < KOREAN_RATIO_THRESHOLD;
    }

    private boolean isNoisyToken(String token) {
        if (token.matches(".*[a-zA-Z].*\\d.*") || token.matches(".*\\d.*[a-zA-Z].*")) {
            return true;
        }
        if (token.length() >= 5 && token.matches("[a-zA-Z]+")) {
            long vowels = token.chars().filter(c -> "aeiouAEIOU".indexOf(c) >= 0).count();
            double vowelRatio = (double) vowels / token.length();
            return vowelRatio < 0.15 || vowelRatio > 0.7;
        }
        return false;
    }

    private double calculateKoreanRatio(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        long korean = text.codePoints().filter(c -> c >= 0xAC00 && c <= 0xD7A3).count();
        long total = text.codePoints().filter(c -> !Character.isWhitespace(c)).count();
        return total > 0 ? (double) korean / total : 0.0;
    }

    @SuppressWarnings("unchecked")
    private String runVisionOcr(File imageFile) {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            log.info("OPENAI_API_KEY 미설정, Vision skip");
            return "";
        }

        try {
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:image/png;base64," + base64Image;

            Map<String, Object> message = Map.of(
                    "role", "user",
                    "content", List.of(
                            Map.of("type", "text", "text",
                                    "이 이미지의 한국어 뉴스 기사 본문을 정확히 추출해주세요. " +
                                            "고유명사(인명, 회사명)와 숫자를 정확하게 인식하고, " +
                                            "추출한 본문 텍스트만 그대로 반환하세요. 설명이나 부연 없이 본문만 출력해주세요."),
                            Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                    )
            );

            Map<String, Object> requestBody = Map.of(
                    "model", VISION_MODEL,
                    "messages", List.of(message),
                    "max_tokens", 2000,
                    "temperature", 0.0
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = visionRestTemplate.postForEntity(VISION_API_URL, request, Map.class);

            return parseVisionResponse(response.getBody());

        } catch (Exception e) {
            log.warn("Vision OCR 실패: {}", e.getMessage());
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private String parseVisionResponse(Map<String, Object> body) {
        if (body == null) return "";
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) return "";
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> messageObj = (Map<String, Object>) firstChoice.get("message");
            if (messageObj == null) return "";
            String content = (String) messageObj.get("content");
            return content != null ? content.trim() : "";
        } catch (Exception e) {
            log.warn("Vision 응답 파싱 실패: {}", e.getMessage());
            return "";
        }
    }
}

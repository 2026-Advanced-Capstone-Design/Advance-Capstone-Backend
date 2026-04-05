package com.factcheck.service;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PreprocessService {

    private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^가-힣a-zA-Z0-9\\s.,!?%()\\-\"']");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    public List<String> preprocess(String rawText) {
        String cleaned = cleanText(rawText);
        List<String> sentences = splitSentences(cleaned);

        List<String> processed = sentences.stream()
                .filter(s -> s.trim().length() > 5)
                .map(this::normalizeWithKomoran)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        log.info("전처리 완료: 원문 {}자 → {}개 문장", rawText.length(), processed.size());
        return processed;
    }

    private String cleanText(String text) {
        text = HTML_TAG.matcher(text).replaceAll(" ");
        text = EMAIL.matcher(text).replaceAll(" ");
        text = URL.matcher(text).replaceAll(" ");
        text = SPECIAL_CHARS.matcher(text).replaceAll(" ");
        text = MULTI_SPACE.matcher(text).replaceAll(" ");
        return text.trim();
    }

    private List<String> splitSentences(String text) {
        String[] split = SENTENCE_SPLIT.split(text);
        return Arrays.stream(split)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private String normalizeWithKomoran(String sentence) {
        try {
            KomoranResult result = komoran.analyze(sentence);
            return result.getTokenList().stream()
                    .filter(token -> {
                        String pos = token.getPos();
                        return pos.startsWith("NN")
                            || pos.startsWith("VV")
                            || pos.startsWith("VA")
                            || pos.equals("MAG");
                    })
                    .map(token -> token.getMorph())
                    .collect(Collectors.joining(" "));
        } catch (Exception e) {
            log.warn("Komoran 분석 실패, 원문 반환: {}", e.getMessage());
            return sentence;
        }
    }

    public List<String> splitOnly(String rawText) {
        String cleaned = cleanText(rawText);
        return splitSentences(cleaned).stream()
                .filter(s -> s.trim().length() > 5)
                .collect(Collectors.toList());
    }
}

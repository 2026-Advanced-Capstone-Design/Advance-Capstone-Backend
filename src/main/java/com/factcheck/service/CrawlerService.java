package com.factcheck.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class CrawlerService {

    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    public Map<String, String> extractFromUrl(String url) {
        Map<String, String> result = new HashMap<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            result.put("title", extractTitle(doc));
            result.put("body", extractBody(doc));
            result.put("publisher", extractPublisher(doc, url));

        } catch (IOException e) {
            log.warn("URL 크롤링 실패: {} - {}", url, e.getMessage());
            result.put("title", "");
            result.put("body", "");
            result.put("publisher", "");
        }

        return result;
    }

    private String extractTitle(Document doc) {
        String ogTitle = doc.select("meta[property=og:title]").attr("content");
        if (!ogTitle.isBlank()) return ogTitle.trim();

        Element h1 = doc.selectFirst("h1");
        if (h1 != null && !h1.text().isBlank()) return h1.text().trim();

        return doc.title().trim();
    }

    private String extractBody(Document doc) {
        String[] bodySelectors = {
                "article",
                "#articleBodyContents",
                "#newsEndContents",
                ".article-body",
                ".news-article-body",
                "#articleContent",
                ".article_body",
                ".article-content",
                "div[itemprop=articleBody]",
        };

        for (String selector : bodySelectors) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                String text = el.text().trim();
                if (text.length() > 100) {
                    return text;
                }
            }
        }

        Elements paragraphs = doc.select("p");
        StringBuilder sb = new StringBuilder();
        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.length() > 20) {
                sb.append(text).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String extractPublisher(Document doc, String url) {
        String siteName = doc.select("meta[property=og:site_name]").attr("content");
        if (!siteName.isBlank()) return siteName.trim();

        try {
            String host = new java.net.URL(url).getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return "";
        }
    }
}

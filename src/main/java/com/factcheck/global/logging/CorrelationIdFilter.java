package com.factcheck.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청마다 correlation id(traceId)를 부여해 MDC에 넣고, 응답 헤더로도 돌려준다.
 * - 클라이언트가 X-Request-Id 헤더를 주면 그대로 사용(프론트~백엔드~AI 추적 연계)
 * - 없으면 8자리 UUID 생성
 * MDC에 들어간 traceId는 logback JSON 로그에 자동 포함되어 Loki에서 한 요청을 끝까지 추적할 수 있다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER_NAME, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}

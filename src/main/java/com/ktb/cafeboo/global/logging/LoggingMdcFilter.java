package com.ktb.cafeboo.global.logging;

import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class LoggingMdcFilter extends OncePerRequestFilter {

    private static final String USER_ID_KEY = "userId";
    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 요청마다 고유한 requestId 부여
            MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString());

            // 인증된 사용자의 userId 설정
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                MDC.put(USER_ID_KEY, String.valueOf(userDetails.getUserId()));
            }

            filterChain.doFilter(request, response);

        } finally {
            // 요청 종료 후 MDC 비우기 (메모리 누수 방지)
            MDC.clear();
        }
    }
}
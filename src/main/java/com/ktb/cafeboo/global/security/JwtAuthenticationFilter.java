package com.ktb.cafeboo.global.security;

import com.ktb.cafeboo.domain.auth.service.TokenBlacklistService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String accessToken = extractAccessToken(request);

        // /refresh 요청은 accessToken 만료 허용, 블랙리스트만 체크
        if (uri.equals("/api/v1/auth/refresh")) {
            if (accessToken != null && tokenBlacklistService.isBlacklisted(accessToken)) {
                handleUnauthorized(response, ErrorStatus.ACCESS_TOKEN_BLACKLISTED);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // 일반 인증 처리
        if (accessToken != null) {
            try {
                authenticateUser(accessToken);
            } catch (CustomApiException e) {
                handleUnauthorized(response, (ErrorStatus) e.getErrorCode());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void authenticateUser(String accessToken) {
        String userIdStr = jwtProvider.validateAccessToken(accessToken); // 만료 시 예외 발생
        if (userIdStr == null || !userIdStr.matches("\\d+")) {
            throw new CustomApiException(ErrorStatus.ACCESS_TOKEN_INVALID);
        }

        if (tokenBlacklistService.isBlacklisted(accessToken)) {
            throw new CustomApiException(ErrorStatus.ACCESS_TOKEN_BLACKLISTED);
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void handleUnauthorized(HttpServletResponse response, ErrorStatus status) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("""
        {
            "status": %s,
            "code": "%s",
            "message": "%s"
        }
        """, status.getStatus(), status.getCode(), status.getMessage()));
    }
}

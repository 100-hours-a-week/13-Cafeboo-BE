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

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.replace("Bearer ", "");

            try {
                String userIdStr = jwtProvider.validateAccessToken(accessToken);

                if (userIdStr == null || !userIdStr.matches("\\d+")) {
                    throw new CustomApiException(ErrorStatus.ACCESS_TOKEN_INVALID);
                }

                // 토큰 블랙리스트 검증
                if (tokenBlacklistService.isBlacklisted(accessToken)) {
                    throw new CustomApiException(ErrorStatus.ACCESS_TOKEN_BLACKLISTED);
                }

                Long userIdLong = Long.parseLong(userIdStr);

                User user = userRepository.findById(userIdLong)
                        .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

                CustomUserDetails userDetails = new CustomUserDetails(user);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (CustomApiException e) {
                logger.warn("[JWT 인증 실패] " + e.getErrorCode().getCode() + ": " + e.getMessage());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(String.format("""
                    {
                        "status": %s
                        "code": "%s",
                        "message": "%s"
                    }
                    """, e.getErrorCode().getStatus(), e.getErrorCode().getCode(), e.getErrorCode().getMessage()));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

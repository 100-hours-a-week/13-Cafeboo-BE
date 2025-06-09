package com.ktb.cafeboo.global.config;

import com.ktb.cafeboo.global.security.JwtAuthenticationFilter;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        List<RequestMatcher> permitAllMatchers = Arrays.asList(
            new AntPathRequestMatcher("/api/v1/auth/oauth"),
            new AntPathRequestMatcher("/api/v1/auth/kakao"),
            new AntPathRequestMatcher("/api/v1/users/email"),
            new AntPathRequestMatcher("/api/v1/reports/weekly/ai_callback"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/index.html"),
            new AntPathRequestMatcher("/actuator/*"),
            new AntPathRequestMatcher("/ws/**"),
            new AntPathRequestMatcher("/css/**"),
            new AntPathRequestMatcher("/js/**"),
            new AntPathRequestMatcher("/images/**"), // 이미지가 있다면 추가
            new AntPathRequestMatcher("/favicon.ico"),
            new AntPathRequestMatcher("/webjars/**"), // SockJS, STOMP.js가 webjars를 통해 제공된다면 필요// 웹소켓 엔드포인트
            new AntPathRequestMatcher("/api/chat/**"),
            new AntPathRequestMatcher("/api/chatrooms/**"),
            new AntPathRequestMatcher("/chatrooms/{roomId}/member") // 테스트용
        );

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 적용
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(permitAllMatchers.toArray(RequestMatcher[]::new)).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "https://jeff-cloud.com", "https://cafeboo.com")); // 프론트 도메인, 서비스 도메인
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // Authorization 헤더 포함 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
package com.ktb.cafeboo.domain.auth.model;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import com.ktb.cafeboo.global.enums.LoginType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OauthToken extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType provider; // KAKAO, GOOGLE 등

    @Column(name = "access_token", nullable = false, length = 2000)
    private String accessToken;

    @Column(name = "refresh_token", length = 2000)
    private String refreshToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public static OauthToken of(User user, String accessToken, String refreshToken, LoginType provider, long expiresInSec) {
        return new OauthToken(user, provider, accessToken, refreshToken, LocalDateTime.now().plusSeconds(expiresInSec));
    }

    private OauthToken(User user, LoginType provider, String accessToken, String refreshToken, LocalDateTime expiresAt) {
        this.user = user;
        this.provider = provider;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }

    public void update(String accessToken, String refreshToken, long expiresInSec) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = LocalDateTime.now().plusSeconds(expiresInSec);
    }
}
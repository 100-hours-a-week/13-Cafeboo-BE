package com.ktb.cafeboo.domain.user.model;

import com.ktb.cafeboo.domain.auth.dto.KakaoUserResponse;
import com.ktb.cafeboo.global.BaseEntity;
import com.ktb.cafeboo.global.enums.LoginType;
import com.ktb.cafeboo.global.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Column
    private String email;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType loginType;

    @Column
    private Long oauthId;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(nullable = false, length = 512)
    private String refreshToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRole role;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean darkMode;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int coffeeBean;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserHealthInfo healthInfo;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserCaffeinInfo caffeinInfo;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserAlarmSetting alarmSetting;

    public static User fromKakao(KakaoUserResponse kakaoUser) {
        User user = new User();
        user.setOauthId(kakaoUser.getId());
        user.setLoginType(LoginType.KAKAO);
        user.setNickname(kakaoUser.getKakaoAccount().getProfile().getNickname());
        user.setRefreshToken("");
        user.setRole(UserRole.USER);
        user.setDarkMode(false);
        user.setCoffeeBean(0);
        return user;
    }

    public void updateRefreshToken(String newToken) {
        this.refreshToken = newToken;
    }

    public void withdraw() {
        // TODO: 유저 기록 삭제 로직
        this.delete();
    }
}

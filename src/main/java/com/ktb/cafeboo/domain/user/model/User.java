package com.ktb.cafeboo.domain.user.model;

import com.ktb.cafeboo.domain.caffeinediary.model.*;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.report.model.*;
import com.ktb.cafeboo.global.infra.kakao.dto.KakaoUserResponse;
import com.ktb.cafeboo.global.BaseEntity;
import com.ktb.cafeboo.global.enums.LoginType;
import com.ktb.cafeboo.global.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Where(clause = "deleted_at IS NULL")
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

    @Column
    private String profileImageUrl;

    @Column(nullable = true, length = 512)
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

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserHealthInfo healthInfo;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserCaffeinInfo caffeinInfo;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserAlarmSetting alarmSetting;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFavoriteDrinkType> favoriteDrinks = new ArrayList<>();

    // 카페인 다이어리 연관관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaffeineIntake> caffeineIntakes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaffeineResidual> caffeineResiduals = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyStatistics> dailyStatisticsList = new ArrayList<>();

    // 리포트 연관관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthlyReport> monthlyReports = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeeklyReport> weeklyReports = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<YearlyReport> yearlyReports = new ArrayList<>();

    @OneToMany(mappedBy = "writer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoffeeChat> coffeeChats = new ArrayList<>();
  
    public static User fromKakao(KakaoUserResponse kakaoUser, String profileImageUrl) {
        User user = new User();
        user.setOauthId(kakaoUser.getId());
        user.setLoginType(LoginType.KAKAO);
        user.setNickname(kakaoUser.getKakaoAccount().getProfile().getNickname());
        user.setRefreshToken("");
        user.setRole(UserRole.USER);
        user.setDarkMode(false);
        user.setCoffeeBean(0);
        user.setProfileImageUrl(profileImageUrl);
        return user;
    }

    public void updateRefreshToken(String newToken) {
        this.refreshToken = newToken;
    }

    public void setFavoriteDrinks(List<UserFavoriteDrinkType> favorites) {
        this.favoriteDrinks.clear(); // 기존 관계 제거
        this.favoriteDrinks.addAll(favorites);
    }

    public void updateProfileImage(String profileImageUrl) {
        if (this.profileImageUrl == null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void addCoffeeBeans(int amount) {
        this.coffeeBean += amount;
    }
}

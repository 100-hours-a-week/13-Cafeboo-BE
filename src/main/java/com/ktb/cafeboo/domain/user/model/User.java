package com.ktb.cafeboo.domain.user.model;

import com.ktb.cafeboo.domain.caffeinediary.model.*;
import com.ktb.cafeboo.domain.drink.model.DrinkType;
import com.ktb.cafeboo.domain.drink.repository.DrinkTypeRepository;
import com.ktb.cafeboo.domain.report.model.*;
import com.ktb.cafeboo.global.infra.kakao.dto.KakaoUserResponse;
import com.ktb.cafeboo.global.BaseEntity;
import com.ktb.cafeboo.global.enums.LoginType;
import com.ktb.cafeboo.global.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Where;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
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

    public void updateFavoriteDrinks(
            List<String> drinkNames,
            DrinkTypeRepository drinkTypeRepository
    ){

        List<String> newFavoriteDrinks = Optional.ofNullable(drinkNames)
                .orElse(List.of()).stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        // 기존 drink 중 요청에 존재하지 않는 항목 삭제
        favoriteDrinks.removeIf(fav ->
                !newFavoriteDrinks.contains(fav.getDrinkType().getName()));

        // 기존 drink 가 아닌 요청 drink 추가
        for(String name : newFavoriteDrinks) {
            boolean exists = favoriteDrinks.stream()
                    .anyMatch(fav -> fav.getDrinkType().getName().equals(name));
            if (!exists) {
                DrinkType drinkType = drinkTypeRepository.findByName(name)
                        .orElseGet(()-> drinkTypeRepository.save(new DrinkType(name)));
                new UserFavoriteDrinkType(this, drinkType);
            }
        }
    }
}

package com.ktb.cafeboo.domain.report.mapper;

import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeRequest;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.drink.model.Drink;
import com.ktb.cafeboo.domain.drink.model.DrinkSizeNutrition;
import com.ktb.cafeboo.domain.report.dto.DailyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.dto.DailyCaffeineReportResponse.HourlyCaffeineInfo;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.user.model.User;
import java.util.List;

public class DailyReportMapper {
    public static CaffeineIntake toEntity(CaffeineIntakeRequest dto, User user, Drink drink, DrinkSizeNutrition drinkSizeNutrition) {
        CaffeineIntake entity = new CaffeineIntake();
        entity.setUser(user);
        entity.setDrink(drink);
        entity.setDrinkSizeNutrition(drinkSizeNutrition);
        entity.setIntakeTime(dto.intakeTime());
        entity.setDrinkCount(dto.drinkCount());
        entity.setCaffeineAmountMg(dto.caffeineAmount());
        return entity;
    }

    public static DailyCaffeineReportResponse toResponse(
        User user, DailyStatistics dailyStatistics, int intakeRate,
        List<HourlyCaffeineInfo> hourlyCaffeineInfoList
    ) {
        return new DailyCaffeineReportResponse(
            user.getNickname(),
            user.getCaffeinInfo().getDailyCaffeineLimitMg(),
            dailyStatistics.getTotalCaffeineMg(),
            intakeRate,
            dailyStatistics.getAiMessage(),
            100F, // 이 100F는 무엇을 의미하는지? 상수로 뽑거나 명확한 의미 부여 필요
            hourlyCaffeineInfoList
        );
    }
}

package com.ktb.cafeboo.domain.caffeinediary.mapper;

import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeRequest;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.drink.model.Drink;
import com.ktb.cafeboo.domain.drink.model.DrinkSizeNutrition;
import com.ktb.cafeboo.domain.user.model.User;

public class CaffeineIntakeMapper {
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

    public static CaffeineIntakeResponse toResponse(CaffeineIntake intake, Drink drink) {
        return new CaffeineIntakeResponse(
            intake.getId().toString(),
            intake.getDrink().getId().toString(),
            drink.getName(),
            intake.getIntakeTime(),
            intake.getDrinkCount(),
            intake.getCaffeineAmountMg()
        );
    }
}

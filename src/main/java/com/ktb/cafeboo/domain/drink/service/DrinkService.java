package com.ktb.cafeboo.domain.drink.service;

import com.ktb.cafeboo.domain.drink.model.Drink;
import com.ktb.cafeboo.domain.drink.model.DrinkSizeNutrition;
import com.ktb.cafeboo.domain.drink.repository.DrinkRepository;
import com.ktb.cafeboo.domain.drink.repository.DrinkSizeNutritionRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.DrinkSize;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DrinkService {
    private final DrinkRepository drinkRepository;
    private final DrinkSizeNutritionRepository drinkSizeNutritionRepository;

    public DrinkSizeNutrition findDrinkSizeNutritionByIdAndSize(Long drinkId, DrinkSize size){
        DrinkSizeNutrition target = drinkSizeNutritionRepository.findByDrinkIdAndSize(drinkId, size)
            .orElseThrow(() -> new CustomApiException(ErrorStatus.DRINK_NOT_FOUND));

        return target;
    }

    public Drink findDrinkById(Long drinkId){
        Drink target = drinkRepository.findById(drinkId)
            .orElseThrow(() -> new CustomApiException(ErrorStatus.DRINK_NOT_FOUND));

        return target;
    }
}

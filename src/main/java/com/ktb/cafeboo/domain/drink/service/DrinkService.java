package com.ktb.cafeboo.domain.drink.service;

import com.ktb.cafeboo.domain.drink.model.Drink;
import com.ktb.cafeboo.domain.drink.model.DrinkSizeNutrition;
import com.ktb.cafeboo.domain.drink.repository.DrinkRepository;
import com.ktb.cafeboo.domain.drink.repository.DrinkSizeNutritionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DrinkService {
    private final DrinkRepository drinkRepository;
    private final DrinkSizeNutritionRepository drinkSizeNutritionRepository;

    public void saveDrink(Drink drink){
        drinkRepository.save(drink);
    }

    public void saveDrinkSizeNutrition(DrinkSizeNutrition drinkSizeNutrition){
        drinkSizeNutritionRepository.save(drinkSizeNutrition);
    }

    public Drink findDrinkById(Long drinkId){
        Drink target = drinkRepository.findById(drinkId)
            .orElseThrow(() -> new IllegalArgumentException("Drink not found"));

        return target;
    }
}

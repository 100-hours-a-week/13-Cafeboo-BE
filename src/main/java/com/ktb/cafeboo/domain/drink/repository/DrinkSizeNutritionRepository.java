package com.ktb.cafeboo.domain.drink.repository;

import com.ktb.cafeboo.domain.drink.model.DrinkSizeNutrition;
import com.ktb.cafeboo.global.enums.DrinkSize;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrinkSizeNutritionRepository extends JpaRepository<DrinkSizeNutrition, Long> {
    Optional<DrinkSizeNutrition> findByIdAndSize(Long id, DrinkSize size);
}


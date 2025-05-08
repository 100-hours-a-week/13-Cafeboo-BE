package com.ktb.cafeboo.domain.drink.repository;

import com.ktb.cafeboo.domain.drink.model.DrinkType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DrinkTypeRepository extends JpaRepository<DrinkType, Long> {
    Optional<DrinkType> findByName(String name);
}
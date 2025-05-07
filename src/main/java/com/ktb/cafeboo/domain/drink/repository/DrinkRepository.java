package com.ktb.cafeboo.domain.drink.repository;

import com.ktb.cafeboo.domain.drink.model.Drink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrinkRepository extends JpaRepository<Drink, Long> {

}

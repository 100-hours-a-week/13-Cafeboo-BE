package com.ktb.cafeboo.domain.drink.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "DrinkSizeNutritions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrinkSizeNutrition extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "drink_id", nullable = false)
    @JsonBackReference
    private Drink drink;

    private String size;

    private Integer capacity_ml;

    private Float caffeine_mg;
}

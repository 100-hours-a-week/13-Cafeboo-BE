package com.ktb.cafeboo.domain.user.model;

import com.ktb.cafeboo.domain.drink.model.Drink;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "user_favorite_drinks")
@Getter
public class UserFavoriteDrinks extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drink_id", nullable = false)
    private Drink drink;
}

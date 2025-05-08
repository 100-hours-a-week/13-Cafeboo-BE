package com.ktb.cafeboo.domain.drink.model;

import com.ktb.cafeboo.domain.user.model.UserFavoriteDrinkType;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drink_types")
@Getter
@Setter
@NoArgsConstructor
public class DrinkType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @OneToMany(mappedBy = "drinkType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFavoriteDrinkType> userFavorites = new ArrayList<>();
}

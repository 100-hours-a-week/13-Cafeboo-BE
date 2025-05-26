package com.ktb.cafeboo.domain.user.model;

import com.ktb.cafeboo.domain.drink.model.DrinkType;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "user_favorite_drink_types")
@Getter
@Setter
@NoArgsConstructor
public class UserFavoriteDrinkType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drink_type_id", nullable = false)
    private DrinkType drinkType;

}

package com.ktb.cafeboo.domain.drink.model;

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
@Table(name = "Drinks")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Drink extends BaseEntity {
    private String name;

    private String type;
}

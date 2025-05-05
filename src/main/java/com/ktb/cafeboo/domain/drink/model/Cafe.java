package com.ktb.cafeboo.domain.drink.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Cafes")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cafe extends BaseEntity {

    private String name;

}

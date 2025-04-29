package com.ktb.cafeboo.domain.caffeinediary.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Table(name = "CaffeineIntakes")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaffineIntake extends BaseEntity {



}

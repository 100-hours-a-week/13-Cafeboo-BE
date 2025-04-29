package com.ktb.cafeboo.domain.caffeinediary.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cafe extends BaseEntity {

    private String name;

}

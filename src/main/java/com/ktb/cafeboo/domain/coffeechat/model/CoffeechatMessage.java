package com.ktb.cafeboo.domain.coffeechat.model;

import com.ktb.cafeboo.global.BaseEntity;
import com.ktb.cafeboo.global.enums.MessageType;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoffeechatMessage extends BaseEntity {
    private String userId;  //테스트용, erd의 Messages 테이블의 user_id. 이후에는 BaseEntity의 id로 대체
    private String roomId;  //테스트용, erd의 Messages 테이블의 id. 이후에는 CoffeChat Entity의 id로 대체
    private String content;
    private MessageType type; //erd 추가 요망
}

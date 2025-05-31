package com.ktb.cafeboo.domain.coffeechat.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

//@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeChat { // 이후에 extends BaseEntity 추가 요망
    private String id; //테스트용 id, 후에 extends BaseEntity 추가될 시 삭제
    private String name; // 채팅방 이름 (예: "자유 채팅방", "스터디 그룹")
    // private List<String> participants; // 참여자 목록 (필요시 추가)
//    private int maxMemberCount;
//    private int currentMemberCount;
//    private String address;
//    private float latitude;
//    private float longitude;

}

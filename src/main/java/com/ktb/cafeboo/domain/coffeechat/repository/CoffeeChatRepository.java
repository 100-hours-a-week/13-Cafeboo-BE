package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 이후 DB 저장 기능 도입 시 수정될 부분. 현재는 기능 테스트용으로 간단하게 만들어서 진행.
 */
@Repository
public class CoffeeChatRepository {
    private final Map<String, CoffeeChat> chatRooms = new LinkedHashMap<>();

    // 초기 채팅방 생성
    public CoffeeChatRepository() {
        createChatRoom("자유 채팅방");
        createChatRoom("개발 스터디");
        createChatRoom("게임 토론방");
    }

    public CoffeeChat createChatRoom(String name) {
        CoffeeChat room = new CoffeeChat(UUID.randomUUID().toString(), name);
        chatRooms.put(String.valueOf(room.getId()), room);
        System.out.println("Chat room created: " + room.getName() + " (ID: " + room.getId() + ")");
        return room;
    }

    public Collection<CoffeeChat> findAllRooms() {
        return chatRooms.values();
    }

    public CoffeeChat findRoomById(String roomId) {
        return chatRooms.get(roomId);
    }
}
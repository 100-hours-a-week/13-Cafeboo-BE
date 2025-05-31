package com.ktb.cafeboo.domain.coffeechat.controller;


import com.ktb.cafeboo.domain.coffeechat.model.Message;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.global.enums.MessageType;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor// HTTP 엔드포인트
public class CoffeeChatController {

    private final CoffeeChatRepository chatRoomRepository;
    private final RedisTemplate<String, Object> redisTemplate; // `Object` 대신 `String`으로 변경할 수도 있음
    private StreamOperations<String, Object, Object> streamOperations; // `Object` 대신 `String, String`으로 변경할 수도 있음

    @PostConstruct
    public void init (){
        this.streamOperations = redisTemplate.opsForStream();
    }

    // 모든 채팅방 목록 조회
    @GetMapping
    public ResponseEntity<Collection<CoffeeChat>> getAllChatRooms() {
        return ResponseEntity.ok(chatRoomRepository.findAllRooms());
    }

    // 특정 채팅방 정보 조회
    @GetMapping("/{roomId}")
    public ResponseEntity<CoffeeChat> getChatRoomById(@PathVariable String roomId) {
        CoffeeChat room = chatRoomRepository.findRoomById(roomId);
        if (room != null) {
            return ResponseEntity.ok(room);
        }
        return ResponseEntity.notFound().build();
    }

    // 특정 채팅방의 이전 메시지 이력 조회 (Redis Stream에서 가져옴)
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<Message>> getChatRoomMessages(@PathVariable String roomId,
        @RequestParam(defaultValue = "0-0") String startId,
        @RequestParam(defaultValue = "100") long count) {
        String chatRoomStreamKey = "coffeechat:" + roomId + ":stream";


        // MapRecord를 읽도록 명시적으로 지정
        List<MapRecord<String, Object, Object>> records = streamOperations.read(
            StreamReadOptions.empty().count(count), // 메시지 개수 제한 옵션
            StreamOffset.create(chatRoomStreamKey, ReadOffset.from(startId)) // 시작 오프셋
        );
//
        List<Message> chatMessages = records.stream()
            .map(record -> {
                // MapRecord의 payload는 Map<Object, Object> 형태로 반환될 수 있음
                // 이를 ChatMessage 객체로 다시 매핑합니다.
                Map<Object, Object> rawData = record.getValue();
                return new Message(
                    (String) rawData.get("senderId"),
                    (String) rawData.get("roomId"),
                    (String) rawData.get("content"),
                    MessageType.valueOf((String) rawData.get("type"))
                );
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(chatMessages);
    }

    // 새로운 채팅방 생성 (예: POST /api/chatrooms?name=새로운방이름)
    @PostMapping
    public ResponseEntity<CoffeeChat> createChatRoom(@RequestParam String name) {
        CoffeeChat newRoom = chatRoomRepository.createChatRoom(name);
        return ResponseEntity.status(201).body(newRoom);
    }
}

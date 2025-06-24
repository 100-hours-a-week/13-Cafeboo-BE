package com.ktb.cafeboo.domain.coffeechat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.cafeboo.domain.coffeechat.dto.StompMessagePublish;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.dto.StompMessage;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.censorship.CensorshipStrategy;
import com.ktb.cafeboo.global.censorship.TextCensorshipFilter;
import com.ktb.cafeboo.global.config.RedisConfig;
import com.ktb.cafeboo.global.enums.MessageType;
import com.ktb.cafeboo.global.infra.redis.stream.listener.RedisStreamListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.connection.stream.StreamInfo.XInfoGroups;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final CoffeeChatRepository chatRoomRepository;
    private StreamOperations<String, Object, Object> streamOperations; // `Object` 대신 `String, String`으로 변경할 수도 있음
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;
    private final RedisStreamListener redisStreamListener;
    private final RedisConfig redisConfig;
    private final CoffeeChatRepository coffeeChatRepository;
    private final CoffeeChatMemberRepository coffeeChatMemberRepository;
    private final CoffeeChatMessageService coffeeChatMessageService;
    private final TextCensorshipFilter textCensorshipFilter;

    private static final String CHAT_STREAM_PREFIX = "coffeechat:room:";
    private static final String CHAT_CONSUMER_GROUP_PREFIX = "coffeechat:group:";

    private static final String PENDING_PROCESSOR_CONSUMER_NAME = "scheduler-pending-processor-" + UUID.randomUUID().toString().substring(0, 8);

    // 각 채팅방에 대한 활성 구독을 관리 (Subscription 객체) 및 중복 구독 방지용 Map
    private final Map<String, Subscription> activeRoomSubscriptions = new ConcurrentHashMap<>();

    // @Scheduled 메서드를 위한 별도의 스케줄러 스레드 풀 (이전과 동일)
    private final ExecutorService pendingMessageProcessor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void setupPendingMessageProcessor(){
        pendingMessageProcessor.submit(this::processPendingMessageScheduler);
    }

    @PostConstruct // 서비스 계층에서 스트림 관련 로직을 초기화하는 것은 자연스럽습니다.
    public void init() {
        this.streamOperations = redisTemplate.opsForStream();
    }

    @PreDestroy
    public void destroy() {
        activeRoomSubscriptions.forEach((roomId, subscription) -> {
            subscription.cancel();
            log.info("[ChatService.destroy] - 채팅방 {}에 대한 Redis Stream의 구독을 취소했습니다.", roomId);
        });
        activeRoomSubscriptions.clear();
        log.info("[ChatService.destroy] - 모든 활성 채팅방에 대한 Redis Stream의 구독을 취소했습니다.");

        // Pending Message를 처리하는 scheduler 종료
        pendingMessageProcessor.shutdown();
        try {
            if (!pendingMessageProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                pendingMessageProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            pendingMessageProcessor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("[ChatService.destroy] - Pending Message를 처리하는 scheduler 종료");
    }

    /**
     * 새로운 message를 Redis Stream에 등록
     * @param message
     */
    public void handleNewMessage(String roomId, StompMessage message) throws Exception{
        String streamKey = CHAT_STREAM_PREFIX + roomId;
        Map<String, String> messageMap = new HashMap<>();

        try{
            String content = message.getMessage();
            Boolean filterResult = textCensorshipFilter.containsBadWord(content, CensorshipStrategy.BOTH);

            if(filterResult){
                log.info("[ChatService.handleMessage()] - 사용자 {} 가 보낸 메시지가 비속적 표현을 포함하고 있습니다", message.getSenderId());
                throw new CustomApiException(ErrorStatus.CENSORED_MESSAGE);
            }

            String senderId = message.getSenderId();
            String coffeechatId = message.getCoffeechatId();

            // CoffeeChat 엔티티 조회
            CoffeeChat chat = coffeeChatRepository.findById(Long.valueOf(coffeechatId))
                .orElseThrow(() -> new IllegalArgumentException("CoffeeChat not found with ID: " + coffeechatId));

            // CoffeeChatMember (메시지 보낸 사람) 엔티티 조회
            // userId와 coffeechatId를 이용해 해당 채팅방의 멤버를 찾습니다.
            CoffeeChatMember sender = coffeeChatMemberRepository.findByCoffeeChatIdAndId(Long.valueOf(coffeechatId), Long.valueOf(senderId))
                .orElseThrow(() -> new IllegalArgumentException("Sender (CoffeeChatMember) not found for user ID: " + senderId + " in chat ID: " + coffeechatId));

            sender.getId();

            //RDB에 메시지 저장
            CoffeeChatMessage coffeeChatMessage = CoffeeChatMessage.builder()
                .messageUuid(UUID.randomUUID().toString()) // 메시지 고유 UUID는 서버에서 생성
                .coffeeChat(chat)                          // 조회한 CoffeeChat 엔티티
                .sender(sender)                            // 조회한 CoffeeChatMember 엔티티
                .content(message.getMessage())      // DTO에서 받은 메시지 내용
                .type(message.getType())            // DTO에서 받은 메시지 타입 (enum)
                .build();

            CoffeeChatMessage savedMessage = coffeeChatMessageService.save(coffeeChatMessage);

            StompMessagePublish messagePublish = StompMessagePublish.from(savedMessage, sender);

            log.info("[ChatService.handleNewMessage] - 직렬화 전 messagePublish 객체 데이터: {}", messagePublish);

            ObjectRecord<String, StompMessagePublish> record =
                ObjectRecord.create(streamKey, messagePublish); // 스트림 키 지정 (필수)

            RecordId recordId = redisTemplate.opsForStream().add(record);
            log.info("[ChatService.handleNewMessage] - Stream {}에 추가된 메시지: {}", streamKey, recordId.getValue());
        }
        catch (Exception e){
            log.error("[ChatService.handleNewMessage] - 메시지 전송 오류. roomId: {}, message: {}", message.getCoffeechatId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 채팅방마다 Redis Stream의 listener를 설정
     */
    public void startListeningToCoffeeChat(String roomId){
        String streamKey = CHAT_STREAM_PREFIX + roomId;
        String consumerGroupName = CHAT_CONSUMER_GROUP_PREFIX + roomId; // 커피챗 ID를 컨슈머 그룹으로 사용
        String consumerName = redisConfig.getConsumerName();
        log.info("[ChatService.startListeningToRoom] - streamKey: {}, consumerGroupName: {}, consumerName: {}", streamKey, consumerGroupName, consumerName);

        // 이미 구독 중이고 서버의 인메모리 맵에 존재하는지 확인
        if (activeRoomSubscriptions.containsKey(roomId)) {
            log.info("[ChatService.startListeningToRoom] - 커피챗 {} 에 대응하는 Stream Listener가 이미 존재합니다.", roomId);
            return;
        }

        // Redis Stream 존재 및 consumer group 존재 여부 확인
        boolean streamExists = false;
        boolean groupExistsInRedis = false;

        try {
            if (redisTemplate.hasKey(streamKey)) {
                streamExists = true;
                XInfoGroups groups = redisTemplate.opsForStream().groups(streamKey);
                for (StreamInfo.XInfoGroup groupInfo : groups) {
                    if (consumerGroupName.equals(groupInfo.groupName())) {
                        groupExistsInRedis = true;
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            log.warn("[ChatService.startListeningToRoom] - Redis Stream 또는 Consumer Group 정보 조회 중 오류 발생 (무시): {}", e.getMessage());
            // 예외 발생 시 groupExistsInRedis를 false로 유지하여 그룹 생성을 시도하게 함 (안전하게)
        }

        if (!groupExistsInRedis) {
            try {
                // RedisTemplate의 createGroup(streamKey, ReadOffset.from("0-0"), consumerGroupName) 오버로드는
                // 스트림이 존재하지 않을 경우 자동으로 생성(MKSTREAM)하는 역할을 겸합니다.
                redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0-0"), consumerGroupName);
                log.info("[ChatService.startListeningToRoom] - Stream {}에 Consumer group {}가 생성되었습니다.", streamKey, consumerGroupName);
                groupExistsInRedis = true; // 새로 생성 성공
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP Consumer Group name already exists")) {
                    log.info("Consumer group {} already exists for stream {}", consumerGroupName, streamKey);
                    groupExistsInRedis = true; // BUSYGROUP이라면 이미 존재하므로 True로 설정
                } else {
                    log.error("Error creating consumer group {}: {}", consumerGroupName, e.getMessage(), e);
                    return; // 다른 심각한 그룹 생성 오류 발생 시 리스너 등록 중단
                }
            }
        } else {
            log.info("[ChatService.startListeningToRoom] - Consumer group {} 이 Redis에 이미 존재합니다. 재사용합니다.", consumerGroupName);
        }

        // 2. ✨ StreamReadRequest를 사용하여 리스너 등록 방식 변경
        StreamReadRequest<String> readRequest = StreamReadRequest.builder(
                StreamOffset.create(streamKey, ReadOffset.lastConsumed())) // 해당 그룹의 마지막으로 소비한 오프셋부터 읽음
            .consumer(Consumer.from(consumerGroupName, consumerName)) // 컨슈머 그룹과 컨슈머 이름
            .autoAcknowledge(true) // onMessage() 성공 시 자동 ACK. 메시지 처리 실패 시 재처리를 위해 이 줄을 제거할 수 있음
            .build();

        // StreamMessageListenerContainer에 리스너 등록
        // 이 Subscription이 XREADGROUP BLOCK 명령을 내부적으로 처리합니다.
        Subscription subscription = streamMessageListenerContainer.register(
            readRequest, // ✨ StreamReadRequest 객체 전달
            redisStreamListener // 메시지 도착 시 onMessage()가 호출될 리스너 인스턴스
        );

        activeRoomSubscriptions.put(roomId, subscription);
        log.info("Stream listener registered for chat room {} with group {} and consumer {}", roomId, consumerGroupName, consumerName);
    }

    // ✨ 기존 CoffeeChatController에 있던 getChatRoomMessages 로직을 여기로 이동
    public List<CoffeeChatMessage> getCoffeechatMessages(String roomId, String startId, long count) {
        String chatRoomStreamKey = "coffeechat:room:" + roomId;

        if (count <= 0) {
            log.info("Requested count is {} for room {}. Returning empty list.", count, roomId);
            return Collections.emptyList();
        }

        log.info("Fetching messages for room {} from ID {} with count {} (reverse order).", roomId, startId, count);

        List<MapRecord<String, Object, Object>> records = streamOperations.reverseRange(
            chatRoomStreamKey,
            Range.of(
                Range.Bound.inclusive("+"),
                Range.Bound.inclusive("-")
            ),
            Limit.limit().count((int) count)
        );
        Collections.reverse(records);
        log.info("[ChatService] - records count: {}", records.size());

        List<CoffeeChatMessage> chatMessages = records.stream()
            .map(record -> {
                // MapRecord의 payload는 Map<Object, Object> 형태로 반환될 수 있음
                // 이를 ChatMessage 객체로 다시 매핑합니다.
                Map<Object, Object> rawData = record.getValue();

                return CoffeeChatMessage.builder()// Enum이라면 String에서 Enum으로 변환 필요
                    .sender((CoffeeChatMember) rawData.get("sender"))
                    .coffeeChat((CoffeeChat) rawData.get("chat"))
                    .content((String) rawData.get("content"))
                    .type(MessageType.valueOf((String) rawData.get("type")))
                    .build();
            })
            .collect(Collectors.toList());

        return chatMessages;
    }

    /**
     * 메시지 전송 과정에서 문제가 생겨 ACK 되지 않은 상태인 pending 메시지 처리 메서드
     */
    @Scheduled(fixedDelay = 300000) // 5분 마다 실행
    @Async
    public void processPendingMessageScheduler(){
        log.info("[ChatService.processPendingMessageScheduler] - PENDING 상태의 메서지 처리 진행...");

        Set<String> activeCoffeeChatIds = activeRoomSubscriptions.keySet();
        if (activeCoffeeChatIds.isEmpty()) {
            log.info("[ChatService.processPendingMessageScheduler] - 활성 채팅방 스트림이 없어 PENDING 메시지 처리를 건너뜝니다.");
            return;
        }

        for(String coffeechatId : activeCoffeeChatIds){
            String streamKey = CHAT_STREAM_PREFIX + coffeechatId;
            String consumerGroupName = CHAT_CONSUMER_GROUP_PREFIX + coffeechatId;

            try{
                PendingMessagesSummary summary = streamOperations.pending(streamKey, consumerGroupName);

                if (summary == null || summary.getTotalPendingMessages() == 0) {
                    log.info("[ChatService.processPendingMessageScheduler] - 스트림 '{}'에 처리할 PENDING 메시지가 없습니다.", streamKey);
                    continue; // 다음 스트림으로 넘어감
                }

                log.info("[ChatService.processPendingMessageScheduler] - 스트림 '{}' (그룹 '{}')의 PENDING 메시지 처리 시작.", streamKey, consumerGroupName);

                PendingMessages allPendingForGroup = streamOperations.pending(streamKey, consumerGroupName, Range.of(Range.Bound.unbounded(), Range.Bound.unbounded()), 10000L);

                if (allPendingForGroup != null) {
                    allPendingForGroup.forEach(pMessage -> {
                        log.info("  - Detail Pending Message ID: {}, Consumer: {}, Elapsed Time: {}ms, Deliveries: {}",
                            pMessage.getId(), pMessage.getConsumerName(), pMessage.getElapsedTimeSinceLastDelivery().toMillis(), pMessage.getTotalDeliveryCount());
                    });
                }

                List<RecordId> messageIdsToClaim = allPendingForGroup.stream()
                    .filter(pMessage -> pMessage.getElapsedTimeSinceLastDelivery().toMillis() > Duration.ofMinutes(1).toMillis()) // 1분 이상 유휴
                    .map(PendingMessage::getId)
                    .collect(Collectors.toList());

                if (messageIdsToClaim.isEmpty())
                    continue;

                List<MapRecord<String, Object, Object>> claimedRecords = streamOperations.claim(
                    streamKey,
                    consumerGroupName,
                    PENDING_PROCESSOR_CONSUMER_NAME,
                    Duration.ofMinutes(1),
                    messageIdsToClaim.toArray(new RecordId[0])
                );

                if (claimedRecords != null && !claimedRecords.isEmpty()) {
                    log.info("[ChatService.processPendingMessageScheduler] - 스트림 '{}'에서 {}개의 메시지를 성공적으로 클레임했습니다. 재처리 진행.", streamKey, claimedRecords.size());
                    for (MapRecord<String, Object, Object> record : claimedRecords) {
                        processAndAcknowledgeMessage(streamKey, consumerGroupName, record);
                    }
                } else {
                    log.info("[ChatService.processPendingMessageScheduler] - 스트림 '{}'에서 클레임할 메시지가 없거나, 이미 다른 컨슈머가 처리 중입니다.", streamKey);
                }
            }
            catch(Exception e){
                log.error("[ChatService.processPendingMessageScheduler] - {}", e.getMessage());
            }
        }
    }

    private void processAndAcknowledgeMessage(String streamKey, String consumerGroupName, MapRecord<String, Object, Object> record) {
        try {
            // MapRecord의 payload는 Map<Object, Object> 형태로 반환될 수 있음
            Map<Object, Object> rawData = record.getValue();
            String messageJson = (String) rawData.get("message");

            if (messageJson == null) {
                log.warn("[ChatService.processAndAcknowledgeMessage] - 처리할 레코드 ID: {} 에 'message' 키가 없습니다. 스킵하고 ACK합니다.", record.getId());
                streamOperations.acknowledge(streamKey, consumerGroupName, record.getId());
                return;
            }

            if (messageJson.startsWith("\"") && messageJson.endsWith("\"")) {
                messageJson = messageJson.substring(1, messageJson.length() - 1);
                messageJson = messageJson.replace("\\\"", "\"");
            }

            StompMessagePublish message = objectMapper.readValue(messageJson, StompMessagePublish.class);

            log.info("[ChatService.processAndAcknowledgeMessage] - 재처리 중인 메시지 ID: {} for room: {} (Content Preview: {})",
                record.getId(), message.getCoffeechatId(), message.getContent().length() > 50 ? message.getContent().substring(0, 50) + "..." : message.getContent());

            messagingTemplate.convertAndSend("/topic/chatrooms/" + message.getCoffeechatId(), message);

            // ✨ 재처리 성공 후 ACK (승인) ✨
            streamOperations.acknowledge(streamKey, consumerGroupName, record.getId());
            log.info("[ChatService.processAndAcknowledgeMessage] - 메시지 ID: {} 를 성공적으로 재처리하고 ACK했습니다.", record.getId());

        } catch (Exception e) {
            log.error("[ChatService.processAndAcknowledgeMessage] - 메시지 ID: {} (스트림: {}) 재처리 및 ACK 중 오류 발생: {}",
                record.getId(), streamKey, e.getMessage(), e);

            streamOperations.acknowledge(streamKey, consumerGroupName, record.getId()); // 임시 ACK
        }
    }
}

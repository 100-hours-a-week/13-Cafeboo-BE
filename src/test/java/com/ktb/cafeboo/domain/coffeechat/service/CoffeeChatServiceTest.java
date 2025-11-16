package com.ktb.cafeboo.domain.coffeechat.service;

import com.ktb.cafeboo.domain.coffeechat.dto.*;
import com.ktb.cafeboo.domain.coffeechat.dto.common.LocationDto;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.domain.tag.service.TagService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.*;
import com.ktb.cafeboo.global.infra.s3.S3Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoffeeChatServiceTest {

    @Mock
    private CoffeeChatRepository coffeeChatRepository;

    @Mock
    private CoffeeChatMemberRepository coffeeChatMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TagService tagService;

    @Mock
    private ChatService chatService;

    @Mock
    private S3Properties s3Properties;

    @InjectMocks
    private CoffeeChatService coffeeChatService;

    private User createMockUser(Long id) {
        User user = User.builder()
                .nickname("윤주")
                .role(UserRole.USER)
                .loginType(LoginType.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private CoffeeChat createMockChat(Long id, User user, LocationDto location) {
        CoffeeChat chat = CoffeeChat.builder()
                .writer(user)
                .name("타이틀")
                .meetingTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 30)))
                .maxMemberCount(4)
                .currentMemberCount(0)
                .status(CoffeeChatStatus.ACTIVE)
                .address(location.address())
                .latitude(location.latitude())
                .longitude(location.longitude())
                .kakaoPlaceUrl(location.kakaoPlaceUrl())
                .build();
        ReflectionTestUtils.setField(chat, "id", id);
        return chat;
    }

    private CoffeeChatMember createMockMember(Long memberId, CoffeeChat chat, User user, String nickname, boolean isHost) {
        String profileImageUrl = s3Properties.getDefaultProfileImageUrl();
        CoffeeChatMember member = CoffeeChatMember.of(chat, user, nickname, profileImageUrl, isHost);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private LocationDto createMockLocation() {
        return new LocationDto(
                "분당구 대왕판교로",
                BigDecimal.valueOf(37.5),
                BigDecimal.valueOf(127.0),
                "http://place.kakao.com/123"
        );
    }

    @Test
    void 커피챗_생성_성공() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        Long memberId = 1L;

        User mockUser = createMockUser(userId);

        LocationDto location = createMockLocation();

        CoffeeChatCreateRequest request = new CoffeeChatCreateRequest(
                "타이틀",
                "내용",
                LocalDate.now(),
                LocalTime.of(10, 30),
                4,
                List.of("태그1", "태그2"),
                location,
                "이든",
                ProfileImageType.DEFAULT
        );

        CoffeeChat mockChat = createMockChat(chatId, mockUser, location);
        CoffeeChatMember mockMember = createMockMember(memberId, mockChat, mockUser, request.chatNickname(), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(coffeeChatRepository.save(any())).thenReturn(mockChat);
        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(mockChat));
        when(coffeeChatMemberRepository.save(any())).thenReturn(mockMember);

        // when
        CoffeeChatCreateResponse response = coffeeChatService.create(userId, request);

        // then
        assertNotNull(response);
        assertEquals(chatId.toString(), response.coffeeChatId());

        verify(userRepository).findById(userId);
        verify(coffeeChatRepository).save(any());
        verify(tagService).saveTagsToCoffeeChat(any(), eq(request.tags()));
    }

    @Test
    void 커피챗_생성_존재하지_않는_유저_예외발생() {
        // given
        Long userId = 1L;

        CoffeeChatCreateRequest request = new CoffeeChatCreateRequest(
                "타이틀",
                "내용",
                LocalDate.now(),
                LocalTime.of(10, 30),
                4,
                List.of("태그1", "태그2"),
                createMockLocation(),
                "이든",
                ProfileImageType.DEFAULT
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class,
                () -> coffeeChatService.create(userId, request));

        assertEquals(ErrorStatus.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 커피챗_참여_성공() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        Long memberId = 1L;

        User mockUser = createMockUser(userId);

        LocationDto location = createMockLocation();

        CoffeeChat mockChat = createMockChat(chatId, mockUser, location);

        CoffeeChatJoinRequest request = new CoffeeChatJoinRequest("이든", ProfileImageType.USER);
        CoffeeChatMember mockMember = createMockMember(memberId, mockChat, mockUser, request.chatNickname(), false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(mockChat));
        when(coffeeChatMemberRepository.save(any())).thenReturn(mockMember);

        // when
        CoffeeChatJoinResponse response = coffeeChatService.join(userId, chatId, request);

        // then
        assertNotNull(response);
        assertEquals(memberId.toString(), response.memberId());

        verify(userRepository).findById(userId);
        verify(coffeeChatRepository).findById(chatId);
        verify(coffeeChatMemberRepository).save(any());
    }

    @Test
    void 커피챗_참여_실패_존재하지않는_유저() {
        // given
        Long userId = 1L;
        Long chatId = 1L;

        CoffeeChatJoinRequest request = new CoffeeChatJoinRequest("닉네임", ProfileImageType.DEFAULT);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class,
                () -> coffeeChatService.join(userId, chatId, request));

        assertEquals(ErrorStatus.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 커피챗_목록_조회_ALL_성공() {
        // given
        Long userId = 1L;
        User mockUser = createMockUser(userId);

        LocationDto location = createMockLocation();

        List<CoffeeChat> mockChats = List.of(createMockChat(1L, mockUser, location));
        when(coffeeChatRepository.findAllActiveChats()).thenReturn(mockChats);

        // when
        CoffeeChatListResponse response = coffeeChatService.getCoffeeChatsByStatus(userId, "all");

        // then
        assertEquals("all", response.filter());
        assertEquals(1, response.coffeechats().size());
        verify(coffeeChatRepository).findAllActiveChats();
    }

    @Test
    void 커피챗_목록_조회_JOINED_성공() {
        // given
        Long userId = 1L;
        User mockUser = createMockUser(userId);
        LocationDto location = createMockLocation();

        List<CoffeeChat> mockChats = List.of(createMockChat(2L, mockUser, location));
        when(coffeeChatRepository.findJoinedChats(userId)).thenReturn(mockChats);

        // when
        CoffeeChatListResponse response = coffeeChatService.getCoffeeChatsByStatus(userId, "joined");

        // then
        assertEquals("joined", response.filter());
        assertEquals(1, response.coffeechats().size());
        verify(coffeeChatRepository).findJoinedChats(userId);
    }

    @Test
    void 커피챗_목록_조회_ENDED_성공() {
        // given
        Long userId = 1L;
        User mockUser = createMockUser(userId);
        LocationDto location = createMockLocation();

        List<CoffeeChat> mockChats = List.of(createMockChat(3L, mockUser, location));
        when(coffeeChatRepository.findCompletedChats(userId)).thenReturn(mockChats);

        // when
        CoffeeChatListResponse response = coffeeChatService.getCoffeeChatsByStatus(userId, "ended");

        // then
        assertEquals("ended", response.filter());
        assertEquals(1, response.coffeechats().size());
        verify(coffeeChatRepository).findCompletedChats(userId);
    }

    @Test
    void 커피챗_목록_조회_REVIEWABLE_성공() {
        // given
        Long userId = 1L;
        User mockUser = createMockUser(userId);
        LocationDto location = createMockLocation();

        List<CoffeeChat> mockChats = List.of(createMockChat(4L, mockUser, location));
        when(coffeeChatRepository.findReviewableChats(any(), any())).thenReturn(mockChats);

        // when
        CoffeeChatListResponse response = coffeeChatService.getCoffeeChatsByStatus(userId, "reviewable");

        // then
        assertEquals("reviewable", response.filter());
        assertEquals(1, response.coffeechats().size());
        verify(coffeeChatRepository).findReviewableChats(eq(userId), any());
    }

    @Test
    void 커피챗_목록_조회_유효하지_않은_필터값_예외발생() {
        // given
        Long userId = 1L;
        String invalidFilter = "invalid";

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class, () ->
                coffeeChatService.getCoffeeChatsByStatus(userId, invalidFilter));

        assertEquals(ErrorStatus.INVALID_COFFEECHAT_FILTER, exception.getErrorCode());
    }

    @Test
    void 커피챗_상세조회_성공() {
        // given
        Long userId = 1L;
        Long chatId = 10L;
        Long memberId = 100L;

        User mockUser = createMockUser(userId);
        LocationDto location = createMockLocation();
        CoffeeChat mockChat = createMockChat(chatId, mockUser, location);

        // 참여자 추가 (writer 포함)
        CoffeeChatMember writerMember = createMockMember(memberId, mockChat, mockUser, "윤주", true);
        mockChat.getMembers().add(writerMember);

        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(mockChat));

        // when
        CoffeeChatDetailResponse response = coffeeChatService.getDetail(chatId, userId);

        // then
        assertNotNull(response);
        assertEquals(chatId.toString(), response.coffeeChatId());
        assertEquals("윤주", response.writer().chatNickname());
        assertEquals("타이틀", response.title());
        assertEquals(location.address(), response.location().address());

        verify(coffeeChatRepository).findById(chatId);
    }

    @Test
    void 커피챗_상세조회_존재하지_않는_커피챗_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 10L;

        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.empty());

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class, () ->
                coffeeChatService.getDetail(chatId, userId));

        assertEquals(ErrorStatus.COFFEECHAT_NOT_FOUND, exception.getErrorCode());
        verify(coffeeChatRepository).findById(chatId);
    }

    @Test
    void 커피챗_상세조회_유효하지_않은_작성자_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 10L;

        User mockUser = createMockUser(userId);
        LocationDto location = createMockLocation();
        CoffeeChat mockChat = createMockChat(chatId, mockUser, location);

        // Writer는 있지만 members에 포함되지 않음
        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(mockChat));

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class, () ->
                coffeeChatService.getDetail(chatId, userId));

        assertEquals(ErrorStatus.COFFEECHAT_MEMBER_NOT_FOUND, exception.getErrorCode());
        verify(coffeeChatRepository).findById(chatId);
    }

    @Test
    void 커피챗_나가기_성공() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        Long memberId = 1L;

        User user = createMockUser(userId);
        CoffeeChat chat = createMockChat(chatId, createMockUser(999L), new LocationDto("주소", BigDecimal.ONE, BigDecimal.ONE, "url"));
        CoffeeChatMember member = createMockMember(memberId, chat, user, "닉네임", false);

        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(coffeeChatMemberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when
        coffeeChatService.leaveChat(chatId, memberId, userId);

        // then
        verify(coffeeChatRepository).findById(chatId);
        verify(coffeeChatMemberRepository).findById(memberId);
        verify(coffeeChatMemberRepository).delete(member);
    }

    @Test
    void 커피챗_나가기_존재하지_않는_커피챗_예외발생() {
        // given
        Long chatId = 1L;
        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.empty());

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class, () ->
                coffeeChatService.leaveChat(chatId, 1L, 1L)
        );
        assertEquals(ErrorStatus.COFFEECHAT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 커피챗_나가기_존재하지_않는_멤버_예외발생() {
        // given
        Long chatId = 1L;
        Long memberId = 2L;
        Long userId = 1L;
        CoffeeChat chat = createMockChat(chatId, createMockUser(999L), new LocationDto("주소", BigDecimal.ONE, BigDecimal.ONE, "url"));

        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(coffeeChatMemberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class, () ->
                coffeeChatService.leaveChat(chatId, memberId, userId)
        );
        assertEquals(ErrorStatus.COFFEECHAT_MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 커피챗_나가기_멤버_권한없음_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        Long memberId = 1L;

        User otherUser = createMockUser(999L);
        CoffeeChat chat = createMockChat(chatId, otherUser, new LocationDto("주소", BigDecimal.ONE, BigDecimal.ONE, "url"));
        CoffeeChatMember member = createMockMember(memberId, chat, otherUser, "닉네임", false);

        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(coffeeChatMemberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class, () ->
                coffeeChatService.leaveChat(chatId, memberId, userId)
        );
        assertEquals(ErrorStatus.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void 커피챗_나가기_작성자_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        Long memberId = 1L;

        User writer = createMockUser(userId);
        CoffeeChat chat = createMockChat(chatId, writer, new LocationDto("주소", BigDecimal.ONE, BigDecimal.ONE, "url"));
        CoffeeChatMember member = createMockMember(memberId, chat, writer, "닉네임", true);

        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(coffeeChatMemberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class, () ->
                coffeeChatService.leaveChat(chatId, memberId, userId)
        );
        assertEquals(ErrorStatus.CANNOT_LEAVE_CHAT_OWNER, exception.getErrorCode());
    }

    @Test
    void 커피챗_삭제_성공() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        Long memberId = 1L;

        User user = createMockUser(userId);

        LocationDto location = createMockLocation();

        CoffeeChat chat = createMockChat(chatId, user, location);

        CoffeeChatMember writer = createMockMember(memberId, chat, user,"이든", true);

        CoffeeChatMessage message = CoffeeChatMessage.of(chat, writer, "hello", MessageType.TALK);

        chat.getMessages().add(message);
        chat.getMembers().add(writer);

        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        // when
        coffeeChatService.delete(chatId, userId);

        // then
        verify(coffeeChatRepository).findById(chatId);
        verify(coffeeChatRepository).save(chat);
        assertNotNull(chat.getDeletedAt());
        assertNotNull(writer.getDeletedAt());
    }

    @Test
    void 커피챗_삭제_존재하지_않는_커피챗_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 1L;

        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.empty());

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class, () ->
                coffeeChatService.delete(chatId, userId)
        );

        assertEquals(ErrorStatus.COFFEECHAT_NOT_FOUND, exception.getErrorCode());
        verify(coffeeChatRepository).findById(chatId);
    }

    @Test
    void 커피챗_삭제_작성자가_아님_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        Long memberId = 1L;

        User writerUser = createMockUser(2L); // 다른 유저
        User currentUser = createMockUser(userId);
        LocationDto location = createMockLocation();
        CoffeeChat chat = createMockChat(chatId, writerUser, location);

        CoffeeChatMember writer = createMockMember(memberId, chat, writerUser, "이든", true);
        chat.getMembers().add(writer);

        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class, () ->
                coffeeChatService.delete(chatId, currentUser.getId())
        );

        assertEquals(ErrorStatus.ACCESS_DENIED, exception.getErrorCode());
        verify(coffeeChatRepository).findById(chatId);
    }

    @Test
    void 커피챗_삭제_상태가_ACTIVE가_아님_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        Long memberId = 1L;

        User user = createMockUser(userId);
        LocationDto location = createMockLocation();
        CoffeeChat chat = createMockChat(chatId, user, location);
        ReflectionTestUtils.setField(chat, "status", CoffeeChatStatus.ENDED);

        CoffeeChatMember writer = createMockMember(memberId, chat, user, "이든", true);
        chat.getMembers().add(writer);

        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class, () ->
                coffeeChatService.delete(chatId, userId)
        );

        assertEquals(ErrorStatus.COFFEECHAT_NOT_ACTIVE, exception.getErrorCode());
        verify(coffeeChatRepository).findById(chatId);
    }

    @Test
    void 커피챗_참여자_목록_조회_성공() {
        // given
        Long chatId = 1L;
        Long writerId = 1L;
        Long participantId = 2L;

        User writerUser = createMockUser(writerId);
        User participantUser = createMockUser(participantId);
        LocationDto location = createMockLocation();

        CoffeeChat chat = createMockChat(chatId, writerUser, location);

        CoffeeChatMember writer = createMockMember(101L, chat, writerUser, "유저1", true);
        CoffeeChatMember participant = createMockMember(102L, chat, participantUser, "유저2", false);

        chat.getMembers().addAll(List.of(writer, participant));

        when(coffeeChatRepository.findByIdWithMembers(chatId)).thenReturn(Optional.of(chat));

        // when
        CoffeeChatMembersResponse response = coffeeChatService.getCoffeeChatMembers(chatId);

        // then
        assertEquals(chatId.toString(), response.coffeeChatId());
        assertEquals(2, response.totalMemberCounts());
        assertEquals("유저1", response.members().get(0).chatNickname()); // 호스트가 먼저
        assertEquals("유저2", response.members().get(1).chatNickname());

        verify(coffeeChatRepository).findByIdWithMembers(chatId);
    }

    @Test
    void 커피챗_멤버_조회_실패_존재하지_않는_커피챗() {
        // given
        Long chatId = 999L; // 존재하지 않는 ID 가정

        when(coffeeChatRepository.findByIdWithMembers(chatId))
                .thenReturn(Optional.empty());

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class,
                () -> coffeeChatService.getCoffeeChatMembers(chatId));

        assertEquals(ErrorStatus.COFFEECHAT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 커피챗_참여_존재하지_않는_커피챗_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        User mockUser = createMockUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.empty());

        CoffeeChatJoinRequest request = new CoffeeChatJoinRequest("이든", ProfileImageType.DEFAULT);

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class,
                () -> coffeeChatService.join(userId, chatId, request));

        assertEquals(ErrorStatus.COFFEECHAT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 커피챗_참여_비활성_커피챗_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        User user = createMockUser(userId);
        LocationDto location = createMockLocation();
        CoffeeChat chat = createMockChat(chatId, user, location);
        ReflectionTestUtils.setField(chat, "status", CoffeeChatStatus.ENDED);

        CoffeeChatJoinRequest request = new CoffeeChatJoinRequest("닉네임", ProfileImageType.DEFAULT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class,
                () -> coffeeChatService.join(userId, chatId, request));

        assertEquals(ErrorStatus.COFFEECHAT_NOT_ACTIVE, exception.getErrorCode());
    }

    @Test
    void 커피챗_참여_이미_참여한_사용자_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        Long memberId = 1L;
        User user = createMockUser(userId);
        LocationDto location = createMockLocation();
        CoffeeChat chat = createMockChat(chatId, user, location);
        CoffeeChatMember member = createMockMember(memberId, chat, user, "닉네임", false);
        chat.getMembers().add(member);

        CoffeeChatJoinRequest request = new CoffeeChatJoinRequest("닉네임", ProfileImageType.DEFAULT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class,
                () -> coffeeChatService.join(userId, chatId, request));

        assertEquals(ErrorStatus.COFFEECHAT_ALREADY_JOINED, exception.getErrorCode());
    }

    @Test
    void 커피챗_참여_정원_초과_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        User user = createMockUser(userId);
        LocationDto location = createMockLocation();
        CoffeeChat chat = createMockChat(chatId, user, location);
        ReflectionTestUtils.setField(chat, "currentMemberCount", chat.getMaxMemberCount());

        CoffeeChatJoinRequest request = new CoffeeChatJoinRequest("닉네임", ProfileImageType.DEFAULT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class,
                () -> coffeeChatService.join(userId, chatId, request));

        assertEquals(ErrorStatus.COFFEECHAT_CAPACITY_EXCEEDED, exception.getErrorCode());
    }

    @Test
    void 커피챗_참여_닉네임_중복_예외발생() {
        // given
        Long userId = 1L;
        Long chatId = 1L;
        User mockUser = createMockUser(userId);
        CoffeeChat mockChat = createMockChat(chatId, mockUser, createMockLocation());

        CoffeeChatJoinRequest request = new CoffeeChatJoinRequest("이든", ProfileImageType.DEFAULT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(coffeeChatRepository.findById(chatId)).thenReturn(Optional.of(mockChat));
        when(coffeeChatMemberRepository.existsByCoffeeChatIdAndChatNickname(chatId, "이든"))
                .thenReturn(true);

        // when & then
        CustomApiException exception = assertThrows(CustomApiException.class,
                () -> coffeeChatService.join(userId, chatId, request));

        assertEquals(ErrorStatus.CHAT_NICKNAME_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void 커피챗_리스너_시작_정상_호출() {
        // given
        String coffeechatId = "123";

        // when
        coffeeChatService.startListeningToCoffeeChat(coffeechatId);

        // then
        verify(chatService, times(1)).startListeningToCoffeeChat(coffeechatId);
    }
}
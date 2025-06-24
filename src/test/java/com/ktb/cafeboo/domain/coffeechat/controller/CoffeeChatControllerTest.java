package com.ktb.cafeboo.domain.coffeechat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.cafeboo.domain.auth.service.TokenBlacklistService;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatCreateRequest;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatCreateResponse;
import com.ktb.cafeboo.domain.coffeechat.dto.common.LocationDto;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatMemberService;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatMessageService;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.enums.LoginType;
import com.ktb.cafeboo.global.enums.ProfileImageType;
import com.ktb.cafeboo.global.enums.UserRole;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import org.h2.engine.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CoffeeChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class CoffeeChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoffeeChatService coffeeChatService;

    @MockitoBean
    private CoffeeChatMessageService coffeeChatMessageService;

    @MockitoBean
    private CoffeeChatMemberService coffeeChatMemberService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private void setAuthentication(Long userId) {
        User user = User.builder()
                .nickname("윤주")
                .role(UserRole.USER)
                .loginType(LoginType.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @DisplayName("커피챗 생성 성공 시 201 Created 응답을 반환")
    @Test
    @WithMockUser
    void 커피챗_생성_성공() throws Exception {
        // given
        setAuthentication(123L);

        CoffeeChatCreateRequest request = new CoffeeChatCreateRequest(
                "제목", "내용", LocalDate.now(), LocalTime.of(10, 30),
                4, List.of("커피", "스터디"),
                new LocationDto("서울", BigDecimal.ONE, BigDecimal.ONE, "https://place.kakao.com"),
                "닉네임", ProfileImageType.DEFAULT
        );

        CoffeeChatCreateResponse response = new CoffeeChatCreateResponse("1");

        when(coffeeChatService.create(anyLong(), any())).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/coffee-chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper()
                            .findAndRegisterModules()
                            .writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COFFEECHAT_CREATE_SUCCESS"))
                .andExpect(jsonPath("$.data.coffeeChatId").value("1"));

        verify(coffeeChatService).create(anyLong(), any());
    }

    @DisplayName("커피챗 생성 중 예외 발생 시 500 Internal Server Error 응답을 반환")
    @Test
    @WithMockUser
    void 커피챗_생성_예외_발생() throws Exception {
        // given
        CoffeeChatCreateRequest request = new CoffeeChatCreateRequest(
                "제목", "내용", LocalDate.now(), LocalTime.of(10, 30),
                4, List.of("커피", "스터디"),
                new LocationDto("서울", BigDecimal.ONE, BigDecimal.ONE, "https://place.kakao.com"),
                "닉네임", ProfileImageType.DEFAULT
        );

        when(coffeeChatService.create(anyLong(), any()))
                .thenThrow(new RuntimeException("서비스 예외 발생"));

        // when & then
        mockMvc.perform(post("/api/v1/coffee-chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper()
                            .findAndRegisterModules()
                            .writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}

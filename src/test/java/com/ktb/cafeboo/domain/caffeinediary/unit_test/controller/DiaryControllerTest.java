package com.ktb.cafeboo.domain.caffeinediary.unit_test.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ktb.cafeboo.domain.auth.service.TokenBlacklistService;
import com.ktb.cafeboo.domain.caffeinediary.controller.CaffeineIntakeController;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeRequest;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.DailyCaffeineDiaryResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.MonthlyCaffeineDiaryResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.MonthlyCaffeineDiaryResponse.DailyIntake;
import com.ktb.cafeboo.domain.caffeinediary.dto.MonthlyCaffeineDiaryResponse.Filter;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineIntakeRepository;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineIntakeService;
import com.ktb.cafeboo.domain.drink.model.Drink;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.LoginType;
import com.ktb.cafeboo.global.enums.UserRole;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CaffeineIntakeController.class)
@AutoConfigureMockMvc(addFilters = false)
class DiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaffeineIntakeService caffeineIntakeService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private User user;
    private Drink mockDrink;

    private void setAuthentication(Long userId) {
        user = User.builder()
            .nickname("테스트")
            .role(UserRole.USER)
            .loginType(LoginType.KAKAO)
            .build();
        ReflectionTestUtils.setField(user, "id", userId);

        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup(){
        setAuthentication(1L);
        objectMapper.registerModule(new JavaTimeModule());

        mockDrink = Drink.builder()
            .name("아메리카노")
            .build();
    }

    @Nested
    @DisplayName("카페인 섭취 기록 (POST /api/v1/caffeine-intakes)")
    class RecordCaffeineIntakeTests {
        @Test
        @DisplayName("카페인 기록 성공 - 200 OK")
        void 카페인_기록_성공() throws Exception {
            // given
            CaffeineIntakeRequest request = new CaffeineIntakeRequest(
                "10", LocalDateTime.now(),1,100f, "GRANDE");

            CaffeineIntakeResponse response = new CaffeineIntakeResponse(
                "1", "10", "아메리카노", request.intakeTime(), 1, 150f);

            when(caffeineIntakeService.recordCaffeineIntake(1L, request)).thenReturn(response);

            // when
            ResultActions result = mockMvc.perform(post("/api/v1/caffeine-intakes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(request)
                ))
                .andExpect(status().isCreated());

            // then
            verify(caffeineIntakeService, times(1)).recordCaffeineIntake(eq(1L), any(CaffeineIntakeRequest.class));

            result.andExpect(jsonPath("$.code").value(SuccessStatus.CAFFEINE_INTAKE_RECORDED.getCode()))
                .andExpect(jsonPath("$.message").value(SuccessStatus.CAFFEINE_INTAKE_RECORDED.getMessage()))
                .andExpect(jsonPath("$.data.id").value(response.id()))
                .andExpect(jsonPath("$.data.drinkId").value(response.drinkId()))
                .andExpect(jsonPath("$.data.drinkName").value(response.drinkName()))
                .andExpect(jsonPath("$.data.drinkCount").value(response.drinkCount()))
                .andExpect(jsonPath("$.data.caffeineAmount").value(response.caffeineAmount()));

        }

        @Test
        @DisplayName("카페인 섭취 내역 등록 실패 - 400 error")
        void 카페인_기록_필수_필드_누락_시_실패() throws Exception {
            // given
            // drinkId가 null인 유효하지 않은 요청 생성
            CaffeineIntakeRequest invalidRequest = new CaffeineIntakeRequest(
                null, LocalDateTime.now(), 1, 100f, "GRANDE");

            // 여기서는 'this.caffeineIntakeService' (Mock 객체)가 예외를 던지도록 설정합니다.
            when(caffeineIntakeService.recordCaffeineIntake(1L, invalidRequest))
                .thenThrow(new CustomApiException(ErrorStatus.INVALID_INTAKE_INFO));

            // when
            ResultActions result = mockMvc.perform(post("/api/v1/caffeine-intakes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(invalidRequest)
                    ))
                .andExpect(status().isBadRequest());

            // then
            result.andExpect(jsonPath("$.code").value(ErrorStatus.INVALID_INTAKE_INFO.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.INVALID_INTAKE_INFO.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(caffeineIntakeService, times(1)).recordCaffeineIntake(1L, invalidRequest);
        }

        @Test
        @DisplayName("카페인 섭취 내역 등록 실패 - 500 error")
        void 카페인_기록_실패_서버_오류() throws Exception {
            // given
            // drinkId가 null인 유효하지 않은 요청 생성
            CaffeineIntakeRequest invalidRequest = new CaffeineIntakeRequest(
                null, LocalDateTime.now(), 1, 100f, "GRANDE");

            // 여기서는 'this.caffeineIntakeService' (Mock 객체)가 예외를 던지도록 설정합니다.
            doThrow(new RuntimeException("알 수 없는 오류"))
                .when(caffeineIntakeService).recordCaffeineIntake(1L, invalidRequest);

            // when
            ResultActions result = mockMvc.perform(post("/api/v1/caffeine-intakes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(invalidRequest)
                    ))
                .andExpect(status().isInternalServerError());

            // then
            result.andExpect(jsonPath("$.code").value(ErrorStatus.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist()
                );

            verify(caffeineIntakeService, times(1)).recordCaffeineIntake(1L, invalidRequest);
        }
    }

    @Nested
    @DisplayName("카페인 섭취 기록 삭제 (DELETE /api/v1/caffeine-intakes)")
    class DeleteCaffeineIntakeTests{
        @Test
        @DisplayName("카페인 섭취 기록 삭제 성공")
        void 카페인_섭취_기록_삭제_성공() throws Exception {
            // given
            Long intakeIdToDelete = 1L; // 삭제할 카페인 섭취 기록 ID

            // mock 서비스의 deleteCaffeineIntake 메소드가 호출될 때 아무것도 하지 않도록 설정
            doNothing().when(caffeineIntakeService).deleteCaffeineIntake(intakeIdToDelete);

            // when
            ResultActions result = mockMvc.perform(delete("/api/v1/caffeine-intakes/{intakeId}", intakeIdToDelete)
                    .contentType(MediaType.APPLICATION_JSON)
                    )
                .andExpect(status().isNoContent());

            // then
            result.andExpect(jsonPath("$").doesNotExist());

            // mock 서비스의 deleteCaffeineIntake 메소드가 정확히 한 번 호출되었는지 검증
            verify(caffeineIntakeService, times(1)).deleteCaffeineIntake(intakeIdToDelete);
        }

        @Test
        @DisplayName("카페인 섭취 기록 삭제 실패 - 해당되는 섭취 내역 없음")
        void 카페인_섭취_기록_삭제_실패() throws Exception {
            // given
            Long intakeIdToDelete = 1L; // 삭제할 카페인 섭취 기록 ID

            doThrow(new CustomApiException(ErrorStatus.INTAKE_NOT_FOUND))
                .when(caffeineIntakeService).deleteCaffeineIntake(intakeIdToDelete);

            // when
            // 컨트롤러의 삭제 메소드 호출
            ResultActions result = mockMvc.perform(delete("/api/v1/caffeine-intakes/{intakeId}", intakeIdToDelete)
                    .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());

            // then
            result.andExpect(jsonPath("$.code").value(ErrorStatus.INTAKE_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorStatus.INTAKE_NOT_FOUND.getMessage()))
                    .andExpect(jsonPath("$.data").doesNotExist());

            // mock 서비스의 deleteCaffeineIntake 메소드가 정확히 한 번 호출되었는지 검증
            verify(caffeineIntakeService, times(1)).deleteCaffeineIntake(intakeIdToDelete);
        }

        @Test
        @DisplayName("카페인 섭취 내역 삭제 실패 - 500 error")
        void 카페인_기록_삭제_서버_오류() throws Exception {
            // given
            Long intakeIdToDelete = 1L; // 삭제할 카페인 섭취 기록 ID

            // drinkId가 null인 유효하지 않은 요청 생성
            CaffeineIntakeRequest invalidRequest = new CaffeineIntakeRequest(
                null, LocalDateTime.now(), 1, 100f, "GRANDE");

            // 여기서는 'this.caffeineIntakeService' (Mock 객체)가 예외를 던지도록 설정합니다.
            doThrow(new RuntimeException("알 수 없는 오류"))
                .when(caffeineIntakeService).deleteCaffeineIntake(intakeIdToDelete);

            // when
            ResultActions result = mockMvc.perform(delete("/api/v1/caffeine-intakes/{id}", intakeIdToDelete)
                    .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isInternalServerError());

            // then
            result.andExpect(jsonPath("$.code").value(ErrorStatus.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist()
                );

            verify(caffeineIntakeService, times(1)).deleteCaffeineIntake(intakeIdToDelete);
        }
    }

    @Nested
    @DisplayName("카페인 섭취 기록 수정 (PATCH /api/v1/caffeine-intakes)")
    class UpdateCaffeineIntakeTests{
        @Test
        @DisplayName("카페인 섭취 기록 수정 성공")
        void 카페인_섭취_기록_수정_성공() throws Exception {
            // given
            Long intakeIdToUpdate = 1L; // 삭제할 카페인 섭취 기록 ID

            // mock request
            CaffeineIntakeRequest request = new CaffeineIntakeRequest(
                "11", LocalDateTime.now().plusHours(1), 2, 200f, "VENTI");

            CaffeineIntakeResponse response = new CaffeineIntakeResponse(
                intakeIdToUpdate.toString(), "11", "카페라떼", request.intakeTime(), 2, 200f);

            when(caffeineIntakeService.updateCaffeineIntake(intakeIdToUpdate, request))
                .thenReturn(response);

            // when
            ResultActions result = mockMvc.perform(patch("/api/v1/caffeine-intakes/{intakeId}", intakeIdToUpdate)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(request)
                    )
                )
                .andExpect(status().isOk());

            // then
            result.andExpect(jsonPath("$.code").value(SuccessStatus.CAFFEINE_INTAKE_UPDATED.getCode()))
                .andExpect(jsonPath("$.message").value(SuccessStatus.CAFFEINE_INTAKE_UPDATED.getMessage()))
                .andExpect(jsonPath("$.data.id").value(response.id()))
                .andExpect(jsonPath("$.data.drinkId").value(response.drinkId()))
                .andExpect(jsonPath("$.data.drinkName").value(response.drinkName()))
                .andExpect(jsonPath("$.data.drinkCount").value(response.drinkCount()))
                .andExpect(jsonPath("$.data.caffeineAmount").value(response.caffeineAmount()));;

            // mock 서비스의 deleteCaffeineIntake 메소드가 정확히 한 번 호출되었는지 검증
            verify(caffeineIntakeService, times(1)).updateCaffeineIntake(intakeIdToUpdate, request);
        }

        @Test
        @DisplayName("카페인 섭취 기록 수정 실패 - 해당되는 섭취 내역 없음")
        void 카페인_섭취_기록_수정_실패_invalid_id() throws Exception {
            // given
            Long intakeIdToUpdate = 1L; // 삭제할 카페인 섭취 기록 ID

            // mock request
            CaffeineIntakeRequest request = new CaffeineIntakeRequest(
                "11", LocalDateTime.now().plusHours(1), 2, 200f, "VENTI");

            doThrow(new CustomApiException(ErrorStatus.INTAKE_NOT_FOUND))
                .when(caffeineIntakeService).updateCaffeineIntake(intakeIdToUpdate, request);

            // when
            // 컨트롤러의 삭제 메소드 호출
            ResultActions result = mockMvc.perform(patch("/api/v1/caffeine-intakes/{intakeId}", intakeIdToUpdate)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(request)
                    )
                )
                .andExpect(status().isNotFound());

            // then
            result.andExpect(jsonPath("$.code").value(ErrorStatus.INTAKE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.INTAKE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist()
                );

            // mock 서비스의 deleteCaffeineIntake 메소드가 정확히 한 번 호출되었는지 검증
            verify(caffeineIntakeService, times(1)).updateCaffeineIntake(intakeIdToUpdate, request);
        }

        @Test
        @DisplayName("카페인 섭취 기록 수정 실패 - 서버 오류")
        void 카페인_섭취_기록_수정_실패_server_error() throws Exception {
            // given
            Long intakeIdToUpdate = 1L; // 삭제할 카페인 섭취 기록 ID

            // mock request
            CaffeineIntakeRequest request = new CaffeineIntakeRequest(
                "11", LocalDateTime.now().plusHours(1), 2, 200f, "VENTI");

            doThrow(new RuntimeException("서비스 내부에서 알 수 없는 오류 발생"))
                .when(caffeineIntakeService).updateCaffeineIntake(intakeIdToUpdate, request);

            // when
            ResultActions result = mockMvc.perform(patch("/api/v1/caffeine-intakes/{intakeId}", intakeIdToUpdate)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(request)
                    )
                )
                .andExpect(status().isInternalServerError());

            // then
            result.andExpect(jsonPath("$.code").value(ErrorStatus.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist()
                );

            // mock 서비스의 deleteCaffeineIntake 메소드가 정확히 한 번 호출되었는지 검증
            verify(caffeineIntakeService, times(1)).updateCaffeineIntake(intakeIdToUpdate, request);
        }
    }

    @Nested
    @DisplayName("카페인 다이어리 달력 조회 (GET /api/v1/caffeine-intakes/monthly)")
    class getCaffeineIntakeDiaryTests{

        @Test
        @DisplayName("카페인 다이어리 달력 조회 성공 - 200")
        void 카페인_다이어리_달력_조회_성공() throws Exception {
            //given
            String targetYear = "2025";
            String targetMonth = "7";

            List<DailyIntake> dailyIntakes = new ArrayList<>();
            dailyIntakes.add(new DailyIntake("2025-07-01", 150.5f));
            dailyIntakes.add(new DailyIntake("2025-07-02", 200.0f));
            dailyIntakes.add(new DailyIntake("2025-07-03", 0.0f));

            MonthlyCaffeineDiaryResponse response = new MonthlyCaffeineDiaryResponse(
                new Filter(targetYear, targetMonth),
                dailyIntakes
            );

            //when
            when(caffeineIntakeService.getCaffeineIntakeDiary(1L, targetYear, targetMonth))
                .thenReturn(response);

            ResultActions result = mockMvc.perform(get("/api/v1/caffeine-intakes/monthly")
                .param("year", targetYear)
                .param("month", targetMonth)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isOk()) // HTTP 200 OK 검증
                .andExpect(jsonPath("$.code").value(SuccessStatus.MONTHLY_CAFFEINE_CALENDAR_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(SuccessStatus.MONTHLY_CAFFEINE_CALENDAR_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.filter.year").value(targetYear))
                .andExpect(jsonPath("$.data.filter.month").value(targetMonth))
                .andExpect(jsonPath("$.data.dailyIntakeList").isArray())
                .andExpect(jsonPath("$.data.dailyIntakeList.length()").value(dailyIntakes.size()));

            // dailyIntakeList 내부의 각 요소 검증
            for (int i = 0; i < dailyIntakes.size(); i++) {
                DailyIntake dailyIntake = dailyIntakes.get(i);
                result.andExpect(jsonPath(String.format("$.data.dailyIntakeList[%d].date", i)).value(dailyIntake.date()))
                    .andExpect(jsonPath(String.format("$.data.dailyIntakeList[%d].totalCaffeineMg", i)).value(dailyIntake.totalCaffeineMg()));
            }

            verify(caffeineIntakeService, times(1)).getCaffeineIntakeDiary(1L,  targetYear, targetMonth);

        }

        @Test
        @DisplayName("카페인 다이어리 달력 조회 실패, 필수 파라미터 null or empty - 400")
        void 카페인_다이어리_달력_조회_실패_필수_파라미터_null_or_empty() throws Exception {
            //given
            String targetYear = "2025";
            String targetMonth = "";

            doThrow(new CustomApiException(ErrorStatus.BAD_REQUEST))
                .when(caffeineIntakeService).getCaffeineIntakeDiary(1L, targetYear, targetMonth);

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/caffeine-intakes/monthly")
                .param("year", targetYear)
                .param("month", targetMonth)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorStatus.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.BAD_REQUEST.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(caffeineIntakeService, times(1)).getCaffeineIntakeDiary(1L, targetYear, targetMonth);
        }

        @Test
        @DisplayName("카페인 다이어리 달력 조회 실패, 필수 파라미터 숫자 아님 - 400")
        void 카페인_다이어리_달력_조회_실패_필수_파라미터_정수_아님() throws Exception {
            //given
            String targetYear = "ㅁㄴㅇㄹ";
            String targetMonth = "abc";

            doThrow(new CustomApiException(ErrorStatus.BAD_REQUEST))
                .when(caffeineIntakeService).getCaffeineIntakeDiary(1L, targetYear, targetMonth);

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/caffeine-intakes/monthly")
                .param("year", targetYear)
                .param("month", targetMonth)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorStatus.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.BAD_REQUEST.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(caffeineIntakeService, times(1)).getCaffeineIntakeDiary(1L, targetYear, targetMonth);
        }

        @Test
        @DisplayName("카페인 다이어리 달력 조회 실패, 서버 오류 - 500")
        void 카페인_다이어리_달력_조회_실패_서버_오류() throws Exception {
            //given
            String targetYear = "2025";
            String targetMonth = "7";

            doThrow(new RuntimeException("서비스 내부에서 알 수 없는 오류 발생"))
                .when(caffeineIntakeService).getCaffeineIntakeDiary(1L, targetYear, targetMonth);

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/caffeine-intakes/monthly")
                .param("year", targetYear)
                .param("month", targetMonth)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorStatus.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(caffeineIntakeService, times(1)).getCaffeineIntakeDiary(1L, targetYear, targetMonth);
        }
    }

    @Nested
    @DisplayName("카페인 다이어리 일별 조회 (GET /api/v1/caffeine-intakes/daily)")
    class getDailyCaffeineIntake{
        @Test
        @DisplayName("카페인 다이어리 일별 조회 성공 - 200")
        void 카페인_다이어리_일별_조회_성공() throws Exception {
            //given
            String date = "2025-07-01";

            List<DailyCaffeineDiaryResponse.IntakeDetail> mockIntakeDetails = new ArrayList<>();
            mockIntakeDetails.add(new DailyCaffeineDiaryResponse.IntakeDetail(
                "101", "drink1", "아메리카노", 1, 150.0f, "2025-06-25T09:00:00"
            ));
            mockIntakeDetails.add(new DailyCaffeineDiaryResponse.IntakeDetail(
                "102", "drink2", "카페라떼", 2, 200.0f, "2025-06-25T14:00:00"
            ));

            DailyCaffeineDiaryResponse response = new DailyCaffeineDiaryResponse(
                new DailyCaffeineDiaryResponse.Filter(date),
                350.0f, // 150 + 200
                mockIntakeDetails
            );

            //when
            when(caffeineIntakeService.getDailyCaffeineIntake(1L, date))
                .thenReturn(response);

            ResultActions result = mockMvc.perform(get("/api/v1/caffeine-intakes/daily")
                .param("date", date)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isOk()) // HTTP 200 OK 검증
                .andExpect(jsonPath("$.code").value(SuccessStatus.DAILY_CAFFEINE_CALENDAR_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(SuccessStatus.DAILY_CAFFEINE_CALENDAR_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.filter.date").value(date))
                .andExpect(jsonPath("$.data.totalCaffeineMg").value(response.totalCaffeineMg()))
                .andExpect(jsonPath("$.data.intakeList").isArray())
                .andExpect(jsonPath("$.data.intakeList.length()").value(response.intakeList().size()));

            // intakeList 내부의 각 요소 검증
            for (int i = 0; i < mockIntakeDetails.size(); i++) {
                DailyCaffeineDiaryResponse.IntakeDetail detail = mockIntakeDetails.get(i);
                result.andExpect(jsonPath(String.format("$.data.intakeList[%d].intakeId", i)).value(detail.intakeId()))
                    .andExpect(jsonPath(String.format("$.data.intakeList[%d].drinkId", i)).value(detail.drinkId()))
                    .andExpect(jsonPath(String.format("$.data.intakeList[%d].drinkName", i)).value(detail.drinkName()))
                    .andExpect(jsonPath(String.format("$.data.intakeList[%d].drinkCount", i)).value(detail.drinkCount()))
                    .andExpect(jsonPath(String.format("$.data.intakeList[%d].caffeineMg", i)).value(detail.caffeineMg()))
                    .andExpect(jsonPath(String.format("$.data.intakeList[%d].intakeTime", i)).value(detail.intakeTime()));
            }


            verify(caffeineIntakeService, times(1)).getDailyCaffeineIntake(1L, date);

        }

        @Test
        @DisplayName("카페인 다이어리 일별 조회 실패, 필수 파라미터 null or empty - 400")
        void 카페인_다이어리_일별_조회_실패_필수_파라미터_null_or_empty() throws Exception {
            //given
            String date = "";

            doThrow(new CustomApiException(ErrorStatus.BAD_REQUEST))
                .when(caffeineIntakeService).getDailyCaffeineIntake(1L, date);

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/caffeine-intakes/daily")
                .param("date", date)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorStatus.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.BAD_REQUEST.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(caffeineIntakeService, times(1)).getDailyCaffeineIntake(1L, date);
        }

        @Test
        @DisplayName("카페인 다이어리 달력 조회 실패, 유효하지 않은 필수 파라미터 - 400")
        void 카페인_다이어리_일별_조회_실패_필수_파라미터_정수_아님() throws Exception {
            //given
            String date = "2025/07/01";

            doThrow(new CustomApiException(ErrorStatus.BAD_REQUEST))
                .when(caffeineIntakeService).getDailyCaffeineIntake(1L, date);

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/caffeine-intakes/daily")
                .param("date", date)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorStatus.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.BAD_REQUEST.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(caffeineIntakeService, times(1)).getDailyCaffeineIntake(1L, date);
        }

        @Test
        @DisplayName("카페인 다이어리 일별 조회 실패, 서버 오류 - 500")
        void 카페인_다이어리_일별_조회_실패_서버_오류() throws Exception {
            //given
            String date = "2025-07-01";

            List<DailyCaffeineDiaryResponse.IntakeDetail> mockIntakeDetails = new ArrayList<>();
            mockIntakeDetails.add(new DailyCaffeineDiaryResponse.IntakeDetail(
                "101", "drink1", "아메리카노", 1, 150.0f, "2025-06-25T09:00:00"
            ));
            mockIntakeDetails.add(new DailyCaffeineDiaryResponse.IntakeDetail(
                "102", "drink2", "카페라떼", 2, 200.0f, "2025-06-25T14:00:00"
            ));

            DailyCaffeineDiaryResponse response = new DailyCaffeineDiaryResponse(
                new DailyCaffeineDiaryResponse.Filter(date),
                350.0f, // 150 + 200
                mockIntakeDetails
            );

            doThrow(new RuntimeException("서비스 내부에서 알 수 없는 오류 발생"))
                .when(caffeineIntakeService).getDailyCaffeineIntake(1L, date);

            //when

            ResultActions result = mockMvc.perform(get("/api/v1/caffeine-intakes/daily")
                .param("date", date)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorStatus.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(caffeineIntakeService, times(1)).getDailyCaffeineIntake(1L, date);
        }
    }
}


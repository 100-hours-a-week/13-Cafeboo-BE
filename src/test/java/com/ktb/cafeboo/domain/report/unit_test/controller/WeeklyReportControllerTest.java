package com.ktb.cafeboo.domain.report.unit_test.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ktb.cafeboo.domain.auth.service.TokenBlacklistService;
import com.ktb.cafeboo.domain.caffeinediary.controller.CaffeineIntakeController;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineIntakeService;
import com.ktb.cafeboo.domain.report.controller.WeeklyReportController;
import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse.DailyIntakeTotal;
import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse.Filter;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.report.service.WeeklyReportScheduler;
import com.ktb.cafeboo.domain.report.service.WeeklyReportService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.LoginType;
import com.ktb.cafeboo.global.enums.UserRole;
import com.ktb.cafeboo.global.infra.ai.dto.CreateWeeklyAnalysisResponse;
import com.ktb.cafeboo.global.infra.ai.dto.ReceiveWeeklyAnalysisRequest;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@ExtendWith(SpringExtension.class)
@WebMvcTest(WeeklyReportController.class)
@AutoConfigureMockMvc(addFilters = false)
public class WeeklyReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeeklyReportService weeklyReportService;

    @MockitoBean
    private DailyStatisticsService dailyStatisticsService;

    @MockitoBean
    private CaffeineIntakeService intakeService;

    @MockitoBean
    private WeeklyReportScheduler weeklyReportScheduler;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private User user;

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
    }

    @Nested
    @DisplayName("주간 리포트 조회 (GET /api/v1/reports/weekly)")
    @WithMockUser
    class getWeeklyCaffeineReport_tests{

        @Test
        @DisplayName("주간 리포트 조회 성공 - 200")
        void 주간_리포트_조회_성공() throws Exception {
            //given
            String year = "2025";
            String month = "6";
            String week = "26";

            List<DailyStatistics> mockDailyStats = Arrays.asList(
                DailyStatistics.builder()
                    .date(LocalDate.parse("2025-06-23"))
                    .totalCaffeineMg(150.0f)
                    .build(),
                DailyStatistics.builder()
                    .date(LocalDate.parse("2025-06-24"))
                    .totalCaffeineMg(200.0f)
                    .build()
            );

            WeeklyCaffeineReportResponse response = new WeeklyCaffeineReportResponse(
                new Filter(year, month, week), // Filter 레코드
                "2025-W26", // isoWeek
                "2025-06-23", // startDate
                "2025-06-29", // endDate
                350.0f, // weeklyCaffeineTotal
                400,    // dailyCaffeineLimit (예시)
                2,      // overLimitDays (예시)
                50.0f,  // dailyCaffeineAvg (예시)
                Arrays.asList( // DailyIntakeTotal 리스트
                    new DailyIntakeTotal("2025-06-23", 150),
                    new DailyIntakeTotal("2025-06-24", 200)
                ),
                "AI가 분석한 주간 리포트 메시지입니다." // aiMessage
            );

            when(dailyStatisticsService.getDailyStatisticsForWeek(eq(user.getId()), eq(year), eq(month), eq(week)))
                .thenReturn(mockDailyStats);

            when(weeklyReportService.getWeeklyReport(eq(user.getId()), eq(year), eq(month), eq(week), eq(mockDailyStats)))
                .thenReturn(response);

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/reports/weekly")
                .param("year", year)
                .param("month", month)
                .param("week", week)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SuccessStatus.WEEKLY_CAFFEINE_REPORT_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(SuccessStatus.WEEKLY_CAFFEINE_REPORT_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").exists())
                // WeeklyCaffeineReportResponse 필드 검증
                .andExpect(jsonPath("$.data.filter.year").value(response.filter().year()))
                .andExpect(jsonPath("$.data.filter.month").value(response.filter().month()))
                .andExpect(jsonPath("$.data.filter.week").value(response.filter().week()))
                .andExpect(jsonPath("$.data.isoWeek").value(response.isoWeek()))
                .andExpect(jsonPath("$.data.startDate").value(response.startDate()))
                .andExpect(jsonPath("$.data.endDate").value(response.endDate()))
                .andExpect(jsonPath("$.data.weeklyCaffeineTotal").value(response.weeklyCaffeineTotal()))
                .andExpect(jsonPath("$.data.dailyCaffeineLimit").value(response.dailyCaffeineLimit()))
                .andExpect(jsonPath("$.data.overLimitDays").value(response.overLimitDays()))
                .andExpect(jsonPath("$.data.dailyCaffeineAvg").value(response.dailyCaffeineAvg()))
                .andExpect(jsonPath("$.data.aiMessage").value(response.aiMessage()))
                .andExpect(jsonPath("$.data.dailyIntakeTotals.length()").value(response.dailyIntakeTotals().size()));

            result.andExpect(jsonPath("$.data.dailyIntakeTotals[0].date").value(response.dailyIntakeTotals().get(0).date()))
                .andExpect(jsonPath("$.data.dailyIntakeTotals[0].caffeineMg").value(response.dailyIntakeTotals().get(0).caffeineMg()))
                .andExpect(jsonPath("$.data.dailyIntakeTotals[1].date").value(response.dailyIntakeTotals().get(1).date()))
                .andExpect(jsonPath("$.data.dailyIntakeTotals[1].caffeineMg").value(response.dailyIntakeTotals().get(1).caffeineMg()));
        }

        @Test
        @DisplayName("주간 리포트 조회 실패 - 400")
        void 주간_리포트_조회_실패_400() throws Exception {
            //given
            String year = "";
            String month = "6";
            String week = "26";

            doThrow(new CustomApiException(ErrorStatus.BAD_REQUEST))
                .when(dailyStatisticsService).getDailyStatisticsForWeek(eq(user.getId()), eq(year), eq(month), eq(week));

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/reports/weekly")
                .param("year", year)
                .param("month", month)
                .param("week", week)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorStatus.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.BAD_REQUEST.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("주간 리포트 조회 실패 - 500")
        void 주간_리포트_조회_실패_500() throws Exception {
            //given
            String year = "2025";
            String month = "6";
            String week = "26";

            List<DailyStatistics> mockDailyStats = Arrays.asList(
                DailyStatistics.builder()
                    .date(LocalDate.parse("2025-06-23"))
                    .totalCaffeineMg(150.0f)
                    .build(),
                DailyStatistics.builder()
                    .date(LocalDate.parse("2025-06-24"))
                    .totalCaffeineMg(200.0f)
                    .build()
            );

            when(dailyStatisticsService.getDailyStatisticsForWeek(eq(user.getId()), eq(year), eq(month), eq(week)))
                .thenReturn(mockDailyStats);

            doThrow(new RuntimeException("실행 중 문제가 발생했습니다"))
                .when(weeklyReportService).getWeeklyReport(eq(user.getId()), eq(year), eq(month), eq(week), eq(mockDailyStats));

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/reports/weekly")
                .param("year", year)
                .param("month", month)
                .param("week", week)
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorStatus.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
        }
    }


    @Nested
    @DisplayName("주간 리포트 AI 요청 생성 (GET /api/v1/reports/weekly/test)")
    @WithMockUser
    class sendWeeklyCaffeineReportToAI{
        @Test
        @DisplayName("주간 리포트 AI 요청 생성 - 200")
        void 주간_리포트_AI_요청_생성_200() throws Exception {
            //given
            CreateWeeklyAnalysisResponse mockResponse = new CreateWeeklyAnalysisResponse();
            mockResponse.setStatus("success");
            mockResponse.setMessage("요청을 성공적으로 받았습니다");

            when(weeklyReportScheduler.generateWeeklyReports())
                .thenReturn(mockResponse);

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/reports/weekly/test")
                    .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SuccessStatus.REPORT_GENERATION_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(SuccessStatus.REPORT_GENERATION_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.message").value("요청을 성공적으로 받았습니다"))
                .andExpect(jsonPath("$.data.data").doesNotExist());

            verify(weeklyReportScheduler, times(1)).generateWeeklyReports();
        }

        @Test
        @DisplayName("주간 리포트 AI 요청 생성 오류 - 502")
        void 주간_리포트_AI_요청_생성_502() throws Exception {
            //given
            doThrow(new CustomApiException(ErrorStatus.AI_SERVER_ERROR))
                .when(weeklyReportScheduler)
                .generateWeeklyReports();

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/reports/weekly/test")
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(ErrorStatus.AI_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.AI_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(weeklyReportScheduler, times(1)).generateWeeklyReports();
        }

        @Test
        @DisplayName("주간 리포트 AI 요청 생성 오류 - 500")
        void 주간_리포트_AI_요청_생성_500() throws Exception {
            //given
            doThrow(new RuntimeException("요청 중 오류 발생"))
                .when(weeklyReportScheduler)
                .generateWeeklyReports();

            //when
            ResultActions result = mockMvc.perform(get("/api/v1/reports/weekly/test")
                .contentType(MediaType.APPLICATION_JSON));

            //then
            result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorStatus.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(weeklyReportScheduler, times(1)).generateWeeklyReports();
        }
    }

    @Nested
    @DisplayName("주간 리포트 AI 콜백 요청 (GET /api/v1/reports/weekly/ai_callback)")
    @WithMockUser
    class getWeeklyCaffeineReportFromAI{

        @Test
        @DisplayName("주간 리포트 AI 콜백 요청 성공 - 200")
        void ai_callback_success() throws Exception {
            //given
            ReceiveWeeklyAnalysisRequest.ReportDto reportDto = new ReceiveWeeklyAnalysisRequest.ReportDto();
            reportDto.setUserId("testUser1");
            reportDto.setReport("This is a test report.");

            ReceiveWeeklyAnalysisRequest request = new ReceiveWeeklyAnalysisRequest();
            request.setReports(Collections.singletonList(reportDto));

            //when
            doNothing().when(weeklyReportService).updateAiMessage(anyList());

            //then
            mockMvc.perform(post("/api/v1/reports/weekly/ai_callback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SuccessStatus.AI_CALLBACK_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(SuccessStatus.AI_CALLBACK_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(weeklyReportService, times(1)).updateAiMessage(anyList());
        }

        @Test
        @DisplayName("주간 리포트 AI 콜백 요청 실패 - 404")
        void ai_callback_fail_404() throws Exception {
            //given
            ReceiveWeeklyAnalysisRequest.ReportDto reportDto = new ReceiveWeeklyAnalysisRequest.ReportDto();
            reportDto.setUserId("testUser1");
            reportDto.setReport("This is a test report.");

            ReceiveWeeklyAnalysisRequest request = new ReceiveWeeklyAnalysisRequest();
            request.setReports(Collections.singletonList(reportDto));

            //when
            doThrow(new CustomApiException(ErrorStatus.REPORT_NOT_FOUND))
                .when(weeklyReportService).updateAiMessage(anyList());

            //then
            mockMvc.perform(post("/api/v1/reports/weekly/ai_callback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorStatus.REPORT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.REPORT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(weeklyReportService, times(1)).updateAiMessage(anyList());
        }

        @Test
        @DisplayName("주간 리포트 AI 콜백 요청 실패 - 500")
        void ai_callback_fail_500() throws Exception {
            //given
            ReceiveWeeklyAnalysisRequest.ReportDto reportDto = new ReceiveWeeklyAnalysisRequest.ReportDto();
            reportDto.setUserId("testUser1");
            reportDto.setReport("This is a test report.");

            ReceiveWeeklyAnalysisRequest request = new ReceiveWeeklyAnalysisRequest();
            request.setReports(Collections.singletonList(reportDto));

            //when
            doThrow(new RuntimeException("처리 중 서버 오류 발생"))
                .when(weeklyReportService).updateAiMessage(anyList());

            //then
            mockMvc.perform(post("/api/v1/reports/weekly/ai_callback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorStatus.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorStatus.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(weeklyReportService, times(1)).updateAiMessage(anyList());
        }
    }
}

package com.ktb.cafeboo.domain.ai.controller;

import com.ktb.cafeboo.domain.ai.service.DrinkRecommendationService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.ai.dto.CreateDrinkRecommendationResponse;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v2/drink-recommendation")
public class DrinkRecommendationController {

    private final DrinkRecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getRecommendation(@AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = userDetails.getId();

        try{
            CreateDrinkRecommendationResponse response = recommendationService.getRecommendationResult(userId);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessStatus.RECOMMENDATION_GENERATE_SUCCESS, response));
        }
        catch (CustomApiException e){
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.of(ErrorStatus.BAD_REQUEST, null));
        }
        catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.of(ErrorStatus.INTERNAL_SERVER_ERROR, null));
        }
    }
}

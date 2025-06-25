package com.ktb.cafeboo.global.censorship.filters;

import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.ToxicityDetectionRequest;
import com.ktb.cafeboo.global.infra.ai.dto.ToxicityDetectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiCensorshipFilter {

    private final AiServerClient aiServerClient;

    public boolean contains(String text) {
        try {
            ToxicityDetectionRequest request = ToxicityDetectionRequest.builder()
                    .userInput(text)
                    .build();

            ToxicityDetectionResponse response = aiServerClient.detectToxicity(request);

            log.info("[AiCensorshipFilter] AI 응답: status={}, is_toxic={}", response.getStatus(), response.getIsToxic());

            // is_toxic == 0 → 유해
            return response.getIsToxic() == 0;

        } catch (Exception e) {
            log.error("[AiCensorshipFilter] AI 검열 호출 실패", e);
            return false;
        }
    }
}
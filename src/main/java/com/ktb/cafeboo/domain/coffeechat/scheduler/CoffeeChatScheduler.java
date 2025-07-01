package com.ktb.cafeboo.domain.coffeechat.scheduler;

import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.global.enums.CoffeeChatStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoffeeChatScheduler {

    private final CoffeeChatRepository coffeeChatRepository;

    /**
     * 매일 오전 9시에 meetingTime 기준 만료 처리
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void expireOutdatedCoffeeChats() {
        LocalDateTime todayMidnight = LocalDate.now().atStartOfDay();
        int updatedCount = coffeeChatRepository.expireOutdatedChats(
                todayMidnight,
                CoffeeChatStatus.ACTIVE,
                CoffeeChatStatus.ENDED
        );
        log.info("[CoffeeChatScheduler] 만료된 커피챗 {}건 처리 완료", updatedCount);
    }
}

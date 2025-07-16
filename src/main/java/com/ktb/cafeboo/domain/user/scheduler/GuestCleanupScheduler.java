package com.ktb.cafeboo.domain.user.scheduler;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuestCleanupScheduler {

    private final UserRepository userRepository;

    // 매일 오전 10시 (cron 표현식: 초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 10 * * *")
    public void deleteOldGuestUsers() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        List<User> oldGuests = userRepository.findGuestUsersBefore(todayStart);

        if (oldGuests.isEmpty()) {
            log.info("[GuestCleanupScheduler] 삭제 대상 게스트 유저 없음");
            return;
        }

        log.info("[GuestCleanupScheduler] {}명의 오래된 게스트 유저 삭제 시작", oldGuests.size());
        userRepository.deleteAll(oldGuests);
        log.info("[GuestCleanupScheduler] 오래된 게스트 유저 삭제 완료");
    }
}

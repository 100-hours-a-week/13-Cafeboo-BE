package com.ktb.cafeboo.domain.user.repository;

import com.ktb.cafeboo.domain.user.model.UserAlarmSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAlarmSettingRepository extends JpaRepository<UserAlarmSetting, Long> {
    boolean existsByUserId(Long userId);
}

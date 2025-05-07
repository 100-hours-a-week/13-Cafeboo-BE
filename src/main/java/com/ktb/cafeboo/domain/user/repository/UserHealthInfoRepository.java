package com.ktb.cafeboo.domain.user.repository;

import com.ktb.cafeboo.domain.user.model.UserHealthInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHealthInfoRepository extends JpaRepository<UserHealthInfo, Long> {
    boolean existsByUserId(Long userId);
}

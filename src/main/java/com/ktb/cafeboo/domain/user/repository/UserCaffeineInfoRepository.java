package com.ktb.cafeboo.domain.user.repository;

import com.ktb.cafeboo.domain.user.model.UserCaffeinInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCaffeineInfoRepository extends JpaRepository<UserCaffeinInfo, Long> {
    boolean existsByUserId(Long userId);
}

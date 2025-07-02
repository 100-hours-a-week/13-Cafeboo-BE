package com.ktb.cafeboo.domain.user.repository;

import com.ktb.cafeboo.domain.user.model.UserCaffeineInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCaffeineInfoRepository extends JpaRepository<UserCaffeineInfo, Long> {
    boolean existsByUserId(Long userId);
}

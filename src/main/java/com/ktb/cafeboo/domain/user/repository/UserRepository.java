package com.ktb.cafeboo.domain.user.repository;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.enums.LoginType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOauthIdAndLoginType(Long oauthId, LoginType loginType);
}


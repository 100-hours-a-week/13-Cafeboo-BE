package com.ktb.cafeboo.domain.auth.repository;

import com.ktb.cafeboo.domain.auth.model.OauthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthTokenRepository extends JpaRepository<OauthToken, Long> {

    Optional<OauthToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
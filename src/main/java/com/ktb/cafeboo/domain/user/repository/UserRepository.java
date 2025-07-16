package com.ktb.cafeboo.domain.user.repository;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.enums.LoginType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOauthIdAndLoginType(Long oauthId, LoginType loginType);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        WHERE u.loginType = 'GUEST'
        AND u.createdAt < :todayStart
    """)
    List<User> findGuestUsersBefore(@Param("todayStart") LocalDateTime todayStart);

}


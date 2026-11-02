package com.globus.finance.repository;

import com.globus.finance.entity.EmailVerificationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationToken> findByTokenHash(
            String tokenHash
    );

    Optional<EmailVerificationToken> findTopByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    @Modifying
    @Query("""
            DELETE FROM EmailVerificationToken t
            WHERE t.user.id = :userId
            """)
    int deleteAllByUserId(
            @Param("userId") Long userId
    );
}
package com.globus.finance.repository;

import com.globus.finance.entity.PasswordResetToken;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHash(
            String tokenHash
    );

    @Modifying
    @Query("""
            DELETE FROM PasswordResetToken t
            WHERE t.user.id = :userId
            """)
    int deleteAllByUserId(
            @Param("userId") Long userId
    );

    Optional<PasswordResetToken> findTopByUser_IdOrderByCreatedAtDesc(
            Long userId
    );
}
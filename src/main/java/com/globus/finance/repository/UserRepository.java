package com.globus.finance.repository;

import com.globus.finance.entity.User;
import com.globus.finance.entity.Role;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
            ORDER BY u.id
            """)
    List<User> lockUsersByRole(@Param("role") Role role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u FROM User u
            WHERE u.email = :email
            """)
    Optional<User> findByEmailForPasswordReset(
            @Param("email") String email
    );
}
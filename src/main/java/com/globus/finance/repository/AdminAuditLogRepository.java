package com.globus.finance.repository;

import com.globus.finance.entity.AdminAuditLog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAuditLogRepository
        extends JpaRepository<AdminAuditLog, Long> {

    List<AdminAuditLog> findAllByOrderByCreatedAtDesc();

}
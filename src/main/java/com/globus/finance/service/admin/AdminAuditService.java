package com.globus.finance.service.admin;

import com.globus.finance.entity.AdminAuditAction;
import com.globus.finance.entity.AdminAuditLog;
import com.globus.finance.repository.AdminAuditLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository auditLogRepository;

    public AdminAuditService(AdminAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(
            Long adminId,
            String adminEmail,
            Long targetUserId,
            String targetEmail,
            AdminAuditAction action,
            String details
    ) {

        AdminAuditLog auditLog = new AdminAuditLog(
                adminId,
                adminEmail,
                targetUserId,
                targetEmail,
                action,
                details
        );

        auditLogRepository.save(auditLog);
    }
}
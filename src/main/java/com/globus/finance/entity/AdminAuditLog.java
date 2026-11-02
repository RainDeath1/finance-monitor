package com.globus.finance.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "target_email", nullable = false)
    private String targetEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminAuditAction action;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AdminAuditLog() {
    }

    public AdminAuditLog(
            Long adminId,
            String adminEmail,
            Long targetUserId,
            String targetEmail,
            AdminAuditAction action,
            String details) {

        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.targetUserId = targetUserId;
        this.targetEmail = targetEmail;
        this.action = action;
        this.details = details;
    }

    @PrePersist
    public void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public String getTargetEmail() {
        return targetEmail;
    }

    public AdminAuditAction getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
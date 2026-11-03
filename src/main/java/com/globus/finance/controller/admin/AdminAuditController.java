package com.globus.finance.controller.admin;

import com.globus.finance.entity.AdminAuditLog;
import com.globus.finance.repository.AdminAuditLogRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminAuditController {

    private final AdminAuditLogRepository auditLogRepository;

    public AdminAuditController(
            AdminAuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/admin/audit")
    public String showAuditLog(
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model
    )  {

        int pageNumber = Math.max(page, 0);

        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                20,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<AdminAuditLog> auditPage =
                auditLogRepository.findAll(pageRequest);

        model.addAttribute("auditLogs", auditPage.getContent());
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("totalPages", auditPage.getTotalPages());
        model.addAttribute("totalElements", auditPage.getTotalElements());

        return "admin/admin-audit";
    }
}
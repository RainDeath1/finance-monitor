package com.globus.finance.controller.dashboard;

import com.globus.finance.dto.analytics.FinancialMetrics;
import com.globus.finance.service.CurrentUserService;
import com.globus.finance.service.dashboard.AnalyticsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsRestController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public AnalyticsRestController(
            AnalyticsService analyticsService,
            CurrentUserService currentUserService) {

        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/metrics")
    public FinancialMetrics getMetrics() {

        Long userId = currentUserService.getCurrentUserId();

        return analyticsService.getMetrics(userId);
    }
}
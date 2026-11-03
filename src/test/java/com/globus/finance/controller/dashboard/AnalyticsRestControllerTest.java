package com.globus.finance.controller.dashboard;

import com.globus.finance.config.SecurityConfig;
import com.globus.finance.dto.analytics.FinancialMetrics;
import com.globus.finance.repository.UserRepository;
import com.globus.finance.service.CurrentUserService;
import com.globus.finance.service.dashboard.AnalyticsService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsRestController.class)
@Import(SecurityConfig.class)
class AnalyticsRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(
                        get("/api/analytics/metrics")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                analyticsService,
                currentUserService
        );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldReturnFinancialMetrics() throws Exception {

        Long userId = 17L;

        FinancialMetrics metrics = new FinancialMetrics(
                new BigDecimal("450000"),
                new BigDecimal("125000"),
                new BigDecimal("325000")
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(analyticsService.getMetrics(userId))
                .thenReturn(metrics);

        mockMvc.perform(
                        get("/api/analytics/metrics")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(450000))
                .andExpect(jsonPath("$.expenses").value(125000))
                .andExpect(jsonPath("$.balance").value(325000));

        verify(analyticsService).getMetrics(userId);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldNotUseForeignUserId() throws Exception {

        Long currentUserId = 17L;

        when(currentUserService.getCurrentUserId())
                .thenReturn(currentUserId);

        when(analyticsService.getMetrics(currentUserId))
                .thenReturn(new FinancialMetrics(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ));

        mockMvc.perform(
                        get("/api/analytics/metrics")
                                .param("userId", "999")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));

        verify(analyticsService).getMetrics(17L);

        verify(analyticsService, never())
                .getMetrics(999L);
    }
}
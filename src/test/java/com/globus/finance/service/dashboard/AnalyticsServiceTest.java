package com.globus.finance.service.dashboard;

import com.globus.finance.dto.analytics.DailyMetrics;
import com.globus.finance.dto.analytics.MonthlyMetrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void shouldRejectMissingUserId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getMetrics(null)
        );

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void shouldRejectInvalidPeriod() {

        LocalDate date = LocalDate.of(2026, 11, 3);

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getMetricsForPeriod(
                        17L,
                        date,
                        date
                )
        );

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void shouldReturnTwelveMonths() {

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = start.plusYears(1);

        when(jdbcTemplate.query(
                anyString(),
                org.mockito.ArgumentMatchers
                        .<RowMapper<MonthlyMetrics>>any(),
                eq(17L),
                eq(start),
                eq(end)
        )).thenReturn(List.of(
                new MonthlyMetrics(
                        3,
                        new BigDecimal("450000"),
                        new BigDecimal("120000")
                )
        ));

        List<MonthlyMetrics> result =
                analyticsService.getMonthlyMetrics(17L, 2026);

        assertEquals(12, result.size());

        assertEquals(1, result.get(0).month());
        assertEquals(
                BigDecimal.ZERO,
                result.get(0).income()
        );

        assertEquals(3, result.get(2).month());

        assertEquals(
                new BigDecimal("450000"),
                result.get(2).income()
        );

        assertEquals(
                new BigDecimal("120000"),
                result.get(2).expenses()
        );

        assertEquals(12, result.get(11).month());
        assertEquals(
                BigDecimal.ZERO,
                result.get(11).expenses()
        );
    }

    @Test
    void shouldReturnAllDaysOfLeapYearFebruary() {

        LocalDate start = LocalDate.of(2024, 2, 1);
        LocalDate end = start.plusMonths(1);

        when(jdbcTemplate.query(
                anyString(),
                org.mockito.ArgumentMatchers
                        .<RowMapper<DailyMetrics>>any(),
                eq(17L),
                eq(start),
                eq(end)
        )).thenReturn(List.of(
                new DailyMetrics(
                        LocalDate.of(2024, 2, 29),
                        new BigDecimal("10000"),
                        new BigDecimal("3000")
                )
        ));

        List<DailyMetrics> result =
                analyticsService.getDailyMetrics(
                        17L,
                        2024,
                        2
                );

        assertEquals(29, result.size());

        assertEquals(
                LocalDate.of(2024, 2, 1),
                result.get(0).date()
        );

        assertEquals(
                BigDecimal.ZERO,
                result.get(0).income()
        );

        assertEquals(
                LocalDate.of(2024, 2, 29),
                result.get(28).date()
        );

        assertEquals(
                new BigDecimal("10000"),
                result.get(28).income()
        );

        assertEquals(
                new BigDecimal("3000"),
                result.get(28).expenses()
        );
    }
}
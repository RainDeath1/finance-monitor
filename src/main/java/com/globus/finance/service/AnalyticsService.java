
package com.globus.finance.service;

import com.globus.finance.dto.DailyMetrics;
import com.globus.finance.dto.MonthlyMetrics;
import com.globus.finance.dto.FinancialMetrics;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Service
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FinancialMetrics getMetrics(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Идентификатор пользователя обязателен");
        }

        String sql = """
                SELECT
                    COALESCE(SUM(CASE
                        WHEN type = 'INCOME'
                        THEN amount ELSE 0 END), 0) AS income,
                    COALESCE(SUM(CASE
                        WHEN type = 'EXPENSE'
                        THEN amount ELSE 0 END), 0) AS expenses
                FROM transactions
                WHERE user_id = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {

                    BigDecimal income =
                            rs.getBigDecimal("income");

                    BigDecimal expenses =
                            rs.getBigDecimal("expenses");

                    BigDecimal balance =
                            income.subtract(expenses);

                    return new FinancialMetrics(
                            income,
                            expenses,
                            balance
                    );
                },
                userId
        );
    }
    public FinancialMetrics getMetricsForPeriod(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        if (userId == null || startDate == null ||
                endDate == null || !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException(
                    "Некорректные параметры периода");
        }

        String sql = """
            SELECT
                COALESCE(SUM(
                    CASE WHEN type = 'INCOME'
                    THEN amount ELSE 0 END
                ), 0) AS income,

                COALESCE(SUM(
                    CASE WHEN type = 'EXPENSE'
                    THEN amount ELSE 0 END
                ), 0) AS expenses

            FROM transactions

            WHERE user_id = ?
              AND transaction_date >= ?
              AND transaction_date < ?
            """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {

                    BigDecimal income =
                            rs.getBigDecimal("income");

                    BigDecimal expenses =
                            rs.getBigDecimal("expenses");

                    return new FinancialMetrics(
                            income,
                            expenses,
                            income.subtract(expenses)
                    );
                },
                userId,
                startDate,
                endDate
        );
    }
    public List<MonthlyMetrics> getMonthlyMetrics(
            Long userId,
            int year) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Идентификатор пользователя обязателен");
        }

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = startDate.plusYears(1);

        String sql = """
            SELECT
                EXTRACT(MONTH FROM transaction_date)::int AS month,

                COALESCE(SUM(
                    CASE
                        WHEN type = 'INCOME'
                        THEN amount
                        ELSE 0
                    END
                ), 0) AS income,

                COALESCE(SUM(
                    CASE
                        WHEN type = 'EXPENSE'
                        THEN amount
                        ELSE 0
                    END
                ), 0) AS expenses

            FROM transactions

            WHERE user_id = ?
              AND transaction_date >= ?
              AND transaction_date < ?

            GROUP BY 1
            ORDER BY 1
            """;

        List<MonthlyMetrics> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new MonthlyMetrics(
                        rs.getInt("month"),
                        rs.getBigDecimal("income"),
                        rs.getBigDecimal("expenses")
                ),
                userId,
                startDate,
                endDate
        );



        List<MonthlyMetrics> result = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            result.add(new MonthlyMetrics(
                    month,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            ));
        }

        for (MonthlyMetrics row : rows) {
            result.set(row.month() - 1, row);
        }

        return result;
    }

    public List<DailyMetrics> getDailyMetrics(
            Long userId,
            int year,
            int month) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Идентификатор пользователя обязателен");
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);

        String sql = """
            SELECT
                transaction_date::date AS day,

                COALESCE(SUM(
                    CASE
                        WHEN type = 'INCOME'
                        THEN amount
                        ELSE 0
                    END
                ), 0) AS income,

                COALESCE(SUM(
                    CASE
                        WHEN type = 'EXPENSE'
                        THEN amount
                        ELSE 0
                    END
                ), 0) AS expenses

            FROM transactions

            WHERE user_id = ?
              AND transaction_date >= ?
              AND transaction_date < ?

            GROUP BY transaction_date::date
            ORDER BY day
            """;

        List<DailyMetrics> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DailyMetrics(
                        rs.getDate("day").toLocalDate(),
                        rs.getBigDecimal("income"),
                        rs.getBigDecimal("expenses")
                ),
                userId,
                startDate,
                endDate
        );

        List<DailyMetrics> result = new ArrayList<>();

        int daysInMonth = startDate.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {

            result.add(new DailyMetrics(
                    startDate.withDayOfMonth(day),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            ));
        }

        for (DailyMetrics row : rows) {
            int index = row.date().getDayOfMonth() - 1;

            result.set(index, row);
        }

        return result;
    }
}

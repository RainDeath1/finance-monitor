
package com.globus.finance.repository;

import com.globus.finance.entity.Transaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    @EntityGraph(attributePaths = {"category"})
    List<Transaction> findByUserIdOrderByTransactionDateDesc(
            Long userId
    );

    @EntityGraph(attributePaths = {"category"})
    Optional<Transaction> findByIdAndUserId(
            Long id,
            Long userId
    );


    boolean existsByCategory_Id(Long categoryId);

    @EntityGraph(attributePaths = {"category"})
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.transactionDate >= :startDate
          AND t.transactionDate < :endDate
        ORDER BY t.transactionDate ASC, t.id ASC
        """)
    List<Transaction> findForReport(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}

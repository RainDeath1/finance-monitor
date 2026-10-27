
package com.globus.finance.repository;

import com.globus.finance.entity.Transaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

}

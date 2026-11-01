
package com.globus.finance.repository;

import com.globus.finance.entity.Category;
import com.globus.finance.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

    List<Category> findByUser_IdOrderByTypeAscNameAsc(Long userId);

    List<Category> findByUser_IdAndType(
            Long userId,
            TransactionType type
    );

    Optional<Category> findByIdAndUser_Id(
            Long categoryId,
            Long userId
    );

    boolean existsByUser_IdAndNameIgnoreCaseAndType(
            Long userId,
            String name,
            TransactionType type
    );

    boolean existsByUser_IdAndNameIgnoreCaseAndTypeAndIdNot(
            Long userId,
            String name,
            TransactionType type,
            Long categoryId
    );

    List<Category> findByType(TransactionType type);

    boolean existsByNameIgnoreCaseAndType(
            String name,
            TransactionType type
    );


    boolean existsByNameIgnoreCaseAndTypeAndIdNot(
            String name,
            TransactionType type,
            Long id
    );

}


package com.globus.finance.repository;

import com.globus.finance.entity.Category;
import com.globus.finance.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

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

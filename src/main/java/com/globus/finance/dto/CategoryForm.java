
package com.globus.finance.dto;

import com.globus.finance.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CategoryForm {

    @NotBlank(message = "Введите название категории")
    @Size(max = 100, message = "Максимум 100 символов")
    private String name;

    @NotNull(message = "Выберите тип категории")
    private TransactionType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}

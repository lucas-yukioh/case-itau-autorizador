package com.github.lucasyukio.caseitauautorizador.dto.request;

import com.github.lucasyukio.caseitauautorizador.model.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

public record TransactionRequest(
        @NotNull UUID accountId,
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0.01") BigDecimal value,
        @NotNull Currency currency
) {}

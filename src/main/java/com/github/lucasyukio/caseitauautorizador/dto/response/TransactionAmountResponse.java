package com.github.lucasyukio.caseitauautorizador.dto.response;

import java.math.BigDecimal;
import java.util.Currency;

public record TransactionAmountResponse(
        BigDecimal value,
        Currency currency
) {}

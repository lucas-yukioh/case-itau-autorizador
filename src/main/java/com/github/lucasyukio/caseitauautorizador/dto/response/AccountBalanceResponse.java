package com.github.lucasyukio.caseitauautorizador.dto.response;

import java.math.BigDecimal;
import java.util.Currency;

public record AccountBalanceResponse(
        BigDecimal amount,
        Currency currency
) {}

package com.github.lucasyukio.caseitauautorizador.dto.response;

import com.github.lucasyukio.caseitauautorizador.model.enums.TransactionStatus;
import com.github.lucasyukio.caseitauautorizador.model.enums.TransactionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        TransactionAmountResponse amount,
        TransactionStatus status,
        LocalDateTime timestamp,
        AccountResponse account
) {}

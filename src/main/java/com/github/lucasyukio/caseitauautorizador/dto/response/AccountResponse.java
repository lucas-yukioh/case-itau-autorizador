package com.github.lucasyukio.caseitauautorizador.dto.response;

import java.util.UUID;

public record AccountResponse(
        UUID id,
        AccountBalanceResponse balance
) {}

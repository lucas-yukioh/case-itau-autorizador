package com.github.lucasyukio.caseitauautorizador.dto.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record AccountMessage(
        UUID id,
        UUID owner,
        @JsonProperty("created_at") String createdAt,
        String status
) {}

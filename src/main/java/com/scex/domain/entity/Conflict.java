package com.scex.domain.entity;

import java.util.Objects;

public record Conflict(ConflictType type, String symbol, String resolution, boolean requiresManualResolution) {
    public Conflict {
        Objects.requireNonNull(type);
        Objects.requireNonNull(symbol);
        Objects.requireNonNull(resolution);
    }
}

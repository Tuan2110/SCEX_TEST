package com.scex.domain.entity;

import java.util.List;
import java.util.Objects;

public record MergeResult(Watchlist watchlist, List<Conflict> conflicts) {
    public MergeResult {
        Objects.requireNonNull(watchlist);
        conflicts = List.copyOf(conflicts);
    }

    public boolean requiresManualResolution() {
        return conflicts.stream().anyMatch(Conflict::requiresManualResolution);
    }
}

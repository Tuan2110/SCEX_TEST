package com.scex.application.usecase;

import com.scex.domain.entity.MergeResult;
import com.scex.domain.entity.Operation;
import com.scex.domain.entity.Watchlist;
import com.scex.domain.service.WatchlistMergeEngine;

import java.util.List;
import java.util.Objects;

public final class MergeWatchlist {
    private final WatchlistMergeEngine engine;

    public MergeWatchlist(WatchlistMergeEngine engine) {
        this.engine = Objects.requireNonNull(engine);
    }

    public MergeResult execute(Watchlist base, List<Operation> serverOperations, List<Operation> localOperations) {
        return engine.merge(base, serverOperations, localOperations);
    }
}

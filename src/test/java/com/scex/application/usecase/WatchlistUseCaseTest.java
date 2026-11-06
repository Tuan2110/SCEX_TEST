package com.scex.application.usecase;

import com.scex.domain.entity.Item;
import com.scex.domain.entity.Operation;
import com.scex.domain.entity.Watchlist;
import com.scex.domain.service.WatchlistMergeEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WatchlistUseCaseTest {
    private final WatchlistMergeEngine engine = new WatchlistMergeEngine();

    @Test
    void mergingPreservesIndependentChangesFromBothBranches() {
        var base = Watchlist.of(new Item("VNM", "old"));
        var remote = List.of(Operation.add("FPT"));
        var local = List.of(Operation.note("VNM", "new"));
        var result = new MergeWatchlist(engine).execute(base, local, remote);
        assertEquals(List.of("VNM", "FPT"), result.watchlist().symbols());
        assertEquals("new", result.watchlist().find("VNM").orElseThrow().note());
        assertTrue(result.conflicts().isEmpty());
    }
}

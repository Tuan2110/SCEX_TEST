package com.scex.domain;

import org.junit.jupiter.api.Test;
import com.scex.domain.entity.*;
import com.scex.domain.service.WatchlistMergeEngine;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WatchlistMergeEngineTest {
    private final WatchlistMergeEngine engine = new WatchlistMergeEngine();
    private final Watchlist base = Watchlist.of(
            new Item("VNM", "old"), new Item("FPT", "old"),
            new Item("HPG", "old"), new Item("VCB", "old"));

    @Test
    void independentChangesPreserveBothBranches() {
        var remote = List.of(Operation.remove("VNM"), Operation.remove("HPG"), Operation.move("VCB", null));
        var local = List.of(Operation.add("MWG"), Operation.add("SSI"), Operation.add("ACB"));

        var result = engine.merge(base, remote, local);

        assertEquals(List.of("VCB", "FPT", "MWG", "SSI", "ACB"), result.watchlist().symbols());
        assertTrue(result.conflicts().isEmpty());
    }

    @Test
    void bothDevicesAddSameSymbolOnlyOnce() {
        var remote = List.of(Operation.add("SSI"));
        var local = List.of(Operation.add("SSI"));

        var result = engine.merge(base, remote, local);

        assertEquals(1, result.watchlist().symbols().stream().filter("SSI"::equals).count());
        assertFalse(result.requiresManualResolution());
    }

    @Test
    void noteAndIndependentMoveBothSurvive() {
        var remote = List.of(Operation.move("FPT", null));
        var local = List.of(Operation.note("FPT", "buy after earnings"));

        var result = engine.merge(base, remote, local);

        assertEquals("FPT", result.watchlist().symbols().get(0));
        assertEquals("buy after earnings", result.watchlist().find("FPT").orElseThrow().note());
    }

    @Test
    void deletedAnchorUsesNearestSurvivingPredecessor() {
        var remote = List.of(Operation.remove("FPT"));
        var local = List.of(Operation.move("VCB", "FPT"));

        var result = engine.merge(base, remote, local);

        assertEquals(List.of("VNM", "VCB", "HPG"), result.watchlist().symbols());
        assertEquals(ConflictType.MOVE_ANCHOR_REMOVED, result.conflicts().get(0).type());
    }

    @Test
    void remoteDeleteVsLocalNoteKeepsEditedItem() {
        var remote = List.of(Operation.remove("VNM"));
        var local = List.of(Operation.note("VNM", "chờ về 80 thì múc"));

        var result = engine.merge(base, remote, local);

        assertEquals("chờ về 80 thì múc", result.watchlist().find("VNM").orElseThrow().note());
        assertEquals(ConflictType.REMOVE_VS_NOTE, result.conflicts().get(0).type());
        assertFalse(result.requiresManualResolution());
    }

    @Test
    void localDeleteVsRemoteNoteKeepsEditedItem() {
        var remote = List.of(Operation.note("VNM", "buy later"));
        var local = List.of(Operation.remove("VNM"));

        var result = engine.merge(base, remote, local);

        assertEquals("buy later", result.watchlist().find("VNM").orElseThrow().note());
    }

    @Test
    void concurrentDifferentNotesRequireManualResolution() {
        var remote = List.of(Operation.note("VNM", "note A"));
        var local = List.of(Operation.note("VNM", "note B"));

        var result = engine.merge(base, remote, local);

        assertTrue(result.requiresManualResolution());
        assertEquals(ConflictType.BOTH_UPDATED_NOTE, result.conflicts().get(0).type());
    }

    @Test
    void concurrentMovesRequireManualResolution() {
        var remote = List.of(Operation.move("VNM", "HPG"));
        var local = List.of(Operation.move("VNM", "FPT"));

        var result = engine.merge(base, remote, local);

        assertTrue(result.requiresManualResolution());
        assertEquals(ConflictType.BOTH_MOVED, result.conflicts().get(0).type());
    }

    @Test
    void remoteDeleteAndLocalMoveDoesNotResurrectItem() {
        var remote = List.of(Operation.remove("VNM"));
        var local = List.of(Operation.move("VNM", "FPT"));

        var result = engine.merge(base, remote, local);

        assertFalse(result.watchlist().symbols().contains("VNM"));
    }

    @Test
    void addedItemStartsWithoutNote() {
        var remote = List.<Operation>of();
        var local = List.of(Operation.add("SSI"));

        var result = engine.merge(base, remote, local);

        assertEquals("", result.watchlist().find("SSI").orElseThrow().note());
        assertTrue(result.conflicts().isEmpty());
    }

    @Test
    void removedAnchorFallbackSkipsTheMovingItem() {
        var remote = List.of(Operation.remove("FPT"));
        var local = List.of(Operation.move("VNM", "FPT"));

        var result = engine.merge(base, remote, local);

        assertEquals(List.of("VNM", "HPG", "VCB"), result.watchlist().symbols());
    }

    @Test
    void rejectsUpdatingMissingItem() {
        var operations = List.of(Operation.note("SSI", "buy"));

        assertThrows(IllegalArgumentException.class, () -> engine.apply(base, operations));
    }

    @Test
    void rejectsUnknownMoveAnchor() {
        var operations = List.of(Operation.move("VNM", "SSI"));

        assertThrows(IllegalArgumentException.class, () -> engine.apply(base, operations));
    }

    @Test
    void rejectsMovingAfterItself() {
        assertThrows(IllegalArgumentException.class, () -> Operation.move("VNM", "VNM"));
    }

    @Test
    void applyingOperationsDoesNotChangeBase() {
        var operations = List.of(Operation.note("VNM", "new"), Operation.remove("FPT"));

        engine.apply(base, operations);

        assertEquals(List.of("VNM", "FPT", "HPG", "VCB"), base.symbols());
        assertEquals("old", base.find("VNM").orElseThrow().note());
    }

    @Test
    void rejectsDuplicateSymbols() {
        assertThrows(IllegalArgumentException.class,
                () -> Watchlist.of(new Item("VNM", "a"), new Item("VNM", "b")));
    }

    @Test
    void localNoteThenRemovalPreservesConcurrentServerNote() {
        var remote = List.of(Operation.note("VNM", "server"));
        var local = List.of(Operation.note("VNM", "temporary"), Operation.remove("VNM"));

        var result = engine.merge(base, remote, local);

        assertEquals("server", result.watchlist().find("VNM").orElseThrow().note());
        assertEquals(ConflictType.REMOVE_VS_NOTE, result.conflicts().getFirst().type());
    }
}

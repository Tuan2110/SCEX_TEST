package com.scex.domain.service;

import com.scex.domain.entity.Conflict;
import com.scex.domain.entity.ConflictType;
import com.scex.domain.entity.Item;
import com.scex.domain.entity.MergeResult;
import com.scex.domain.entity.Operation;
import com.scex.domain.entity.Type;
import com.scex.domain.entity.Watchlist;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WatchlistMergeEngine {
    public MergeResult merge(Watchlist base,
                             List<Operation> serverOps,
                             List<Operation> localOps) {
        Objects.requireNonNull(base);
        Objects.requireNonNull(serverOps);
        Objects.requireNonNull(localOps);
        Watchlist remote = apply(base, serverOps);
        Watchlist local = apply(base, localOps);
        List<Item> result = new ArrayList<>(remote.items());
        List<Conflict> conflicts = new ArrayList<>();

        for (Operation op : localOps) {
            String s = op.symbol();
            boolean remoteRemoved = base.find(s).isPresent() && remote.find(s).isEmpty();
            boolean localRemoved = base.find(s).isPresent() && local.find(s).isEmpty();
            boolean remoteNoteChanged = noteChanged(base, remote, s);
            boolean localNoteChanged = noteChanged(base, local, s);

            switch (op.type()) {
                case ADD -> {
                    if (indexOf(result, s) < 0) result.add(new Item(s, ""));
                }

                case REMOVE -> {
                    if (remoteNoteChanged && localRemoved) {
                        addOnce(conflicts, new Conflict(ConflictType.REMOVE_VS_NOTE,
                                s, "KEEP_EDITED_ITEM (server note)", false));
                    } else {
                        result.removeIf(x -> x.symbol().equals(s));
                    }
                }
                case UPDATE_NOTE -> {
                    if (localRemoved) continue;
                    if (remoteRemoved && localNoteChanged) {
                        restoreFromBase(result, base, s);
                        replaceNote(result, s, op.note());
                        addOnce(conflicts, new Conflict(ConflictType.REMOVE_VS_NOTE,
                                s, "KEEP_EDITED_ITEM (local note)", false));
                    } else if (remoteNoteChanged && localNoteChanged
                            && !Objects.equals(remote.find(s).orElseThrow().note(),
                            local.find(s).orElseThrow().note())) {
                        addOnce(conflicts, new Conflict(ConflictType.BOTH_UPDATED_NOTE,
                                s, "MANUAL: preserve both alternatives; keep server note provisionally", true));
                    } else if (indexOf(result, s) >= 0) {
                        replaceNote(result, s, op.note());
                    }
                }
                case MOVE -> {
                    if (remoteRemoved) continue;
                    Optional<Operation> serverMove = lastOp(serverOps, s, Type.MOVE);
                    if (serverMove.isPresent()
                            && !Objects.equals(serverMove.get().afterSymbol(), op.afterSymbol())) {
                        addOnce(conflicts, new Conflict(ConflictType.BOTH_MOVED,
                                s, "MANUAL: choose position; keep server position provisionally", true));
                        continue;
                    }
                    if (indexOf(result, s) < 0) continue;
                    String anchor = op.afterSymbol();
                    if (anchor != null && indexOf(result, anchor) < 0) {
                        anchor = fallbackAnchor(base, result, anchor, s);
                        addOnce(conflicts, new Conflict(ConflictType.MOVE_ANCHOR_REMOVED,
                                s, "FALLBACK_TO_NEAREST_SURVIVING_BASE_PREDECESSOR", false));
                    }
                    move(result, s, anchor);
                }
            }
        }
        return new MergeResult(new Watchlist(result), conflicts);
    }

    public Watchlist apply(Watchlist base, List<Operation> operations) {
        Objects.requireNonNull(base);
        Objects.requireNonNull(operations);
        List<Item> items = new ArrayList<>(base.items());
        for (Operation op : operations) {
            String s = op.symbol();
            switch (op.type()) {
                case ADD -> {
                    if (indexOf(items, s) < 0) items.add(new Item(s, ""));
                }
                case REMOVE -> items.removeIf(x -> x.symbol().equals(s));
                case UPDATE_NOTE -> {
                    if (indexOf(items, s) < 0)
                        throw new IllegalArgumentException("Cannot update absent symbol: " + s);
                    replaceNote(items, s, op.note());
                }
                case MOVE -> {
                    if (indexOf(items, s) < 0)
                        throw new IllegalArgumentException("Cannot move absent symbol: " + s);
                    if (op.afterSymbol() != null && indexOf(items, op.afterSymbol()) < 0)
                        throw new IllegalArgumentException("Unknown move anchor: " + op.afterSymbol());
                    move(items, s, op.afterSymbol());
                }
            }
        }
        return new Watchlist(items);
    }

    private static boolean noteChanged(Watchlist base, Watchlist branch, String s) {
        return base.find(s).isPresent() && branch.find(s).isPresent()
                && !Objects.equals(base.find(s).orElseThrow().note(), branch.find(s).orElseThrow().note());
    }

    private static Optional<Operation> lastOp(List<Operation> ops, String s, Type type) {
        for (int i = ops.size() - 1; i >= 0; i--)
            if (ops.get(i).symbol().equals(s) && ops.get(i).type() == type)
                return Optional.of(ops.get(i));
        return Optional.empty();
    }

    private static void addOnce(List<Conflict> conflicts, Conflict conflict) {
        if (conflicts.stream().noneMatch(c -> c.symbol().equals(conflict.symbol())
                && c.type() == conflict.type())) conflicts.add(conflict);
    }

    private static int indexOf(List<Item> items, String symbol) {
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).symbol().equals(symbol)) return i;
        return -1;
    }

    private static void replaceNote(List<Item> items, String symbol, String note) {
        int i = indexOf(items, symbol);
        if (i >= 0) items.set(i, new Item(symbol, note));
    }

    private static void move(List<Item> items, String symbol, String afterSymbol) {
        Item item = items.remove(indexOf(items, symbol));
        int pos = afterSymbol == null ? 0 : indexOf(items, afterSymbol) + 1;
        if (afterSymbol != null && pos == 0)
            throw new IllegalArgumentException("Anchor vanished: " + afterSymbol);
        items.add(pos, item);
    }

    private static String fallbackAnchor(Watchlist base, List<Item> current, String missing, String excludedSymbol) {
        int pos = base.symbols().indexOf(missing);
        if (pos < 0) return null;
        for (int i = pos - 1; i >= 0; i--) {
            String candidate = base.symbols().get(i);
            if (!candidate.equals(excludedSymbol) && indexOf(current, candidate) >= 0) return candidate;
        }
        return null;
    }

    private static void restoreFromBase(List<Item> items, Watchlist base, String symbol) {
        if (indexOf(items, symbol) >= 0) return;
        Item item = base.find(symbol).orElseThrow();
        String predecessor = fallbackAnchor(base, items, symbol, symbol);
        int insertAt = predecessor == null ? 0 : indexOf(items, predecessor) + 1;
        items.add(insertAt, item);
    }
}

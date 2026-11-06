package com.scex.domain.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public record Watchlist(List<Item> items) {
    public Watchlist {
        items = List.copyOf(items);
        var symbols = new HashSet<String>();
        for (Item item : items) {
            if (!symbols.add(item.symbol())) {
                throw new IllegalArgumentException("Duplicate symbol: " + item.symbol());
            }
        }
    }

    public static Watchlist of(Item... items) {
        return new Watchlist(List.of(items));
    }

    public List<String> symbols() {
        return items.stream().map(Item::symbol).toList();
    }

    public Optional<Item> find(String symbol) {
        return items.stream().filter(item -> item.symbol().equals(symbol)).findFirst();
    }
}

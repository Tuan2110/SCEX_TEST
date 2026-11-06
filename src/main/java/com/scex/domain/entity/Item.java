package com.scex.domain.entity;

import java.util.Objects;

public record Item(String symbol, String note) {
    public Item {
        Objects.requireNonNull(symbol);
        Objects.requireNonNull(note);
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Blank symbol");
        }
    }
}

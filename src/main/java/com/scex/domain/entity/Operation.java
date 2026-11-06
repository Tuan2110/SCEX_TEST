package com.scex.domain.entity;

import java.util.Objects;

public record Operation(Type type, String symbol, String note, String afterSymbol) {
    public Operation {
        Objects.requireNonNull(type);
        Objects.requireNonNull(symbol);
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Blank symbol");
        }
        if (type == Type.UPDATE_NOTE) {
            Objects.requireNonNull(note);
        }
        if (type == Type.ADD && note != null) {
            throw new IllegalArgumentException("ADD does not accept a note");
        }
        if (type == Type.MOVE && symbol.equals(afterSymbol)) {
            throw new IllegalArgumentException("Cannot move a symbol after itself");
        }
    }

    public static Operation add(String symbol) {
        return new Operation(Type.ADD, symbol, null, null);
    }

    public static Operation remove(String symbol) {
        return new Operation(Type.REMOVE, symbol, null, null);
    }

    public static Operation note(String symbol, String note) {
        return new Operation(Type.UPDATE_NOTE, symbol, note, null);
    }

    public static Operation move(String symbol, String afterSymbol) {
        return new Operation(Type.MOVE, symbol, null, afterSymbol);
    }

}

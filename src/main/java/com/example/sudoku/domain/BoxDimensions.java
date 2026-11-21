package com.example.sudoku.domain;

public record BoxDimensions(int rows, int columns) {
    public static BoxDimensions forSize(int size) {
        if (size < 2 || size > 16) {
            throw new IllegalArgumentException("Size must be between 2 and 16");
        }
        int rows = (int) Math.sqrt(size);
        while (size % rows != 0) {
            rows--;
        }
        return new BoxDimensions(rows, size / rows);
    }

    public static void validate(int size, int rows, int columns) {
        BoxDimensions expected = forSize(size);
        if (rows != expected.rows || columns != expected.columns) {
            throw new IllegalArgumentException("Region dimensions do not match the board size");
        }
    }
}

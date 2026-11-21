package com.example.sudoku.domain;

public final class Board {
    private final int size;
    private final int[][] cells;

    public Board(int size, int[][] cells) {
        BoxDimensions.forSize(size);
        if (cells == null || cells.length != size) {
            throw new IllegalArgumentException("Board must have exactly " + size + " rows");
        }
        this.size = size;
        this.cells = new int[size][size];
        for (int row = 0; row < size; row++) {
            if (cells[row] == null || cells[row].length != size) {
                throw new IllegalArgumentException("Each board row must have exactly " + size + " cells");
            }
            for (int column = 0; column < size; column++) {
                int value = cells[row][column];
                if (value < 0 || value > size) {
                    throw new IllegalArgumentException("Board values must be between 0 and " + size);
                }
                this.cells[row][column] = value;
            }
        }
    }

    public int size() {
        return size;
    }

    public int get(int row, int column) {
        return cells[row][column];
    }

    public int[][] copyCells() {
        int[][] copy = new int[size][];
        for (int row = 0; row < size; row++) {
            copy[row] = cells[row].clone();
        }
        return copy;
    }
}

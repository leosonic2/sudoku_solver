package com.example.sudoku.domain;

public final class SudokuValidator {
    private SudokuValidator() {
    }

    public static void validateClues(Board puzzle, Board current) {
        if (puzzle.size() != current.size()) {
            throw new IllegalArgumentException("Puzzle and current board sizes must match");
        }
        for (int row = 0; row < puzzle.size(); row++) {
            for (int column = 0; column < puzzle.size(); column++) {
                int clue = puzzle.get(row, column);
                if (clue != 0 && current.get(row, column) != clue) {
                    throw new IllegalArgumentException("Current board must preserve every clue");
                }
            }
        }
    }

    public static boolean hasConflicts(Board board) {
        int size = board.size();
        BoxDimensions boxes = BoxDimensions.forSize(size);
        boolean[][] rows = new boolean[size][size + 1];
        boolean[][] columns = new boolean[size][size + 1];
        boolean[][] regions = new boolean[size][size + 1];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int value = board.get(row, column);
                if (value == 0) {
                    continue;
                }
                int region = (row / boxes.rows()) * (size / boxes.columns()) + column / boxes.columns();
                if (rows[row][value] || columns[column][value] || regions[region][value]) {
                    return true;
                }
                rows[row][value] = true;
                columns[column][value] = true;
                regions[region][value] = true;
            }
        }
        return false;
    }

    public static boolean isCompleteAndValid(Board board) {
        if (hasConflicts(board)) {
            return false;
        }
        for (int row = 0; row < board.size(); row++) {
            for (int column = 0; column < board.size(); column++) {
                if (board.get(row, column) == 0) {
                    return false;
                }
            }
        }
        return true;
    }
}

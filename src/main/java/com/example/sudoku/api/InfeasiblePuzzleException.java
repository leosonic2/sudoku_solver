package com.example.sudoku.api;

final class InfeasiblePuzzleException extends RuntimeException {
    InfeasiblePuzzleException() {
        super("Your current entries make the puzzle unsolvable");
    }
}

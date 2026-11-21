package com.example.sudoku.solver;

public final class SolverLimitException extends RuntimeException {
    public SolverLimitException(String message) {
        super(message);
    }
}

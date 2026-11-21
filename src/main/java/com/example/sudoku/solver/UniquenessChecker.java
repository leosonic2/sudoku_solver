package com.example.sudoku.solver;

import com.example.sudoku.domain.Board;
import com.example.sudoku.domain.SudokuValidator;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class UniquenessChecker {
    public enum Outcome {
        UNIQUE, MULTIPLE, INFEASIBLE, INCONCLUSIVE
    }

    @FunctionalInterface
    interface Search {
        LpSudokuSolver.Result solve(Board board, Board excluded, long timeoutMs);
    }

    private final Search search;

    @Autowired
    public UniquenessChecker(BoundedSudokuSolver solver) {
        this.search = solver::solve;
    }

    UniquenessChecker(Search search) {
        this.search = search;
    }

    public Outcome check(Board board, long timeoutMs) {
        long start = System.nanoTime();
        try {
            if (timeoutMs <= 0) {
                return Outcome.INCONCLUSIVE;
            }
            LpSudokuSolver.Result first = search.solve(board, null, timeoutMs);
            if (first.solution().isEmpty()) {
                return Outcome.INFEASIBLE;
            }
            long remaining = timeoutMs - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            if (remaining <= 0) {
                return Outcome.INCONCLUSIVE;
            }
            return secondSearch(board, first.solution().orElseThrow(), remaining);
        } catch (SolverLimitException limit) {
            return Outcome.INCONCLUSIVE;
        }
    }

    public Outcome checkAgainstSolution(Board board, Board solution, long timeoutMs) {
        SudokuValidator.validateClues(board, solution);
        if (!SudokuValidator.isCompleteAndValid(solution)) {
            throw new IllegalArgumentException("Known solution must be valid and complete");
        }
        if (timeoutMs <= 0) {
            return Outcome.INCONCLUSIVE;
        }
        try {
            return secondSearch(board, solution, timeoutMs);
        } catch (SolverLimitException limit) {
            return Outcome.INCONCLUSIVE;
        }
    }

    private Outcome secondSearch(Board board, Board solution, long timeoutMs) {
        return search.solve(board, solution, timeoutMs).solution().isPresent()
                ? Outcome.MULTIPLE : Outcome.UNIQUE;
    }
}

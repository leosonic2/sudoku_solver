package com.example.sudoku.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.sudoku.domain.Board;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class UniquenessCheckerTest {
    private static final Board SOLUTION = new Board(4, new int[][] {
            {1, 2, 3, 4}, {3, 4, 1, 2}, {2, 1, 4, 3}, {4, 3, 2, 1}
    });

    @Test
    void distinguishesUniqueMultipleAndInfeasibleBoards() {
        UniquenessChecker checker = new UniquenessChecker(new BoundedSudokuSolver(new SolverProperties()));
        Board unique = new Board(4, new int[][] {
                {0, 2, 3, 4}, {3, 4, 1, 2}, {2, 1, 4, 3}, {4, 3, 2, 1}
        });
        Board multiple = new Board(4, new int[4][4]);
        Board infeasible = new Board(4, new int[][] {
                {1, 1, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}
        });
        assertEquals(UniquenessChecker.Outcome.UNIQUE, checker.check(unique, 10_000));
        assertEquals(UniquenessChecker.Outcome.UNIQUE, checker.check(SOLUTION, 10_000));
        assertEquals(UniquenessChecker.Outcome.MULTIPLE, checker.check(multiple, 10_000));
        assertEquals(UniquenessChecker.Outcome.MULTIPLE, checker.checkAgainstSolution(multiple, SOLUTION, 10_000));
        assertEquals(UniquenessChecker.Outcome.INFEASIBLE, checker.check(infeasible, 10_000));
    }

    @Test
    void timeoutOnSecondSearchIsInconclusive() {
        AtomicInteger calls = new AtomicInteger();
        UniquenessChecker checker = new UniquenessChecker((board, excluded, timeout) -> {
            if (calls.getAndIncrement() == 0) {
                return new LpSudokuSolver.Result(Optional.of(SOLUTION), 1);
            }
            assertNotNull(excluded);
            throw new SolverLimitException("Second search timed out");
        });
        assertEquals(UniquenessChecker.Outcome.INCONCLUSIVE, checker.check(new Board(4, new int[4][4]), 10_000));
        assertEquals(2, calls.get());
    }
}

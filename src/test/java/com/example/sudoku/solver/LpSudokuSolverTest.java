package com.example.sudoku.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.sudoku.domain.Board;
import com.example.sudoku.domain.SudokuValidator;
import org.junit.jupiter.api.Test;

class LpSudokuSolverTest {
    private final LpSudokuSolver solver = new LpSudokuSolver();

    @Test
    void solvesFourAndSixBySixPuzzles() {
        assertSolved(new int[][] {
                {1, 0, 0, 4}, {0, 4, 1, 0}, {0, 1, 4, 0}, {4, 0, 0, 1}
        });
        assertSolved(new int[][] {
                {1, 0, 3, 0, 5, 0}, {0, 5, 0, 1, 0, 3},
                {2, 0, 4, 0, 6, 0}, {0, 6, 0, 2, 0, 4},
                {3, 0, 5, 0, 1, 0}, {0, 1, 0, 3, 0, 5}
        });
    }

    @Test
    void solvesAnEasyNineByNinePuzzle() {
        assertSolved(new int[][] {
                {5, 3, 0, 0, 7, 0, 0, 0, 0}, {6, 0, 0, 1, 9, 5, 0, 0, 0},
                {0, 9, 8, 0, 0, 0, 0, 6, 0}, {8, 0, 0, 0, 6, 0, 0, 0, 3},
                {4, 0, 0, 8, 0, 3, 0, 0, 1}, {7, 0, 0, 0, 2, 0, 0, 0, 6},
                {0, 6, 0, 0, 0, 0, 2, 8, 0}, {0, 0, 0, 4, 1, 9, 0, 0, 5},
                {0, 0, 0, 0, 8, 0, 0, 7, 9}
        });
    }

    @Test
    void solvesAHardNineByNinePuzzle() {
        int nodes = assertSolved(new int[][] {
                {1, 0, 0, 0, 0, 7, 0, 9, 0}, {0, 3, 0, 0, 2, 0, 0, 0, 8},
                {0, 0, 9, 6, 0, 0, 5, 0, 0}, {0, 0, 5, 3, 0, 0, 9, 0, 0},
                {0, 1, 0, 0, 8, 0, 0, 0, 2}, {6, 0, 0, 0, 0, 4, 0, 0, 0},
                {3, 0, 0, 0, 0, 0, 0, 1, 0}, {0, 4, 0, 0, 0, 0, 0, 0, 7},
                {0, 0, 7, 0, 0, 0, 3, 0, 0}
        });
        assertTrue(nodes > 1, "The fractional relaxation must trigger branching");
    }

    @Test
    void rejectsConflictsAndImpossibleBoards() {
        Board conflicting = new Board(4, new int[][] {
                {1, 1, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}
        });
        Board impossible = new Board(4, new int[][] {
                {1, 2, 3, 0}, {0, 0, 0, 4}, {0, 0, 0, 0}, {0, 0, 0, 0}
        });
        assertFalse(solver.solve(conflicting).solution().isPresent());
        assertFalse(solver.solve(impossible).solution().isPresent());
    }

    @Test
    void choosesMostFractionalVariable() {
        assertEquals(2, LpSudokuSolver.mostFractional(new double[] {0, 0.1, 0.52, 0.9, 1}));
        assertEquals(-1, LpSudokuSolver.mostFractional(new double[] {0, 1, 1e-7}));
    }

    private int assertSolved(int[][] cells) {
        Board input = new Board(cells.length, cells);
        LpSudokuSolver.Result result = solver.solve(input);
        assertTrue(result.solution().isPresent(), "Expected a solution");
        Board solution = result.solution().orElseThrow();
        SudokuValidator.validateClues(input, solution);
        assertTrue(SudokuValidator.isCompleteAndValid(solution));
        return result.nodesExplored();
    }
}

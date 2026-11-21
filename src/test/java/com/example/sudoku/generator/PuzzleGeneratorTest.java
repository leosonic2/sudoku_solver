package com.example.sudoku.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.sudoku.domain.Board;
import com.example.sudoku.domain.SudokuValidator;
import com.example.sudoku.solver.BoundedSudokuSolver;
import com.example.sudoku.solver.SolverLimitException;
import com.example.sudoku.solver.SolverProperties;
import com.example.sudoku.solver.UniquenessChecker;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PuzzleGeneratorTest {
    @Test
    void generatesUniqueNonemptyPuzzlesAcrossBoardShapes() {
        GeneratorProperties properties = new GeneratorProperties();
        properties.setTimeoutMs(5_000);
        UniquenessChecker checker = new UniquenessChecker(new BoundedSudokuSolver(new SolverProperties()));
        PuzzleGenerator generator = new PuzzleGenerator(properties, checker);
        for (int size : new int[] {4, 6, 9}) {
            Board puzzle = generator.generate(size);
            assertFalse(SudokuValidator.hasConflicts(puzzle));
            assertTrue(countEmpty(puzzle) > 0);
            assertEquals(UniquenessChecker.Outcome.UNIQUE, checker.check(puzzle, 30_000));
        }
    }

    @Test
    void reportsLimitBeforeAnyVerifiedRemoval() {
        GeneratorProperties properties = new GeneratorProperties();
        AtomicLong clock = new AtomicLong();
        PuzzleGenerator generator = new PuzzleGenerator(properties, (puzzle, solution, remaining) -> {
            clock.set(TimeUnit.SECONDS.toNanos(11));
            return UniquenessChecker.Outcome.UNIQUE;
        }, clock::get, () -> new Random(1));
        assertThrows(SolverLimitException.class, () -> generator.generate(4));
    }

    @Test
    void returnsLastVerifiedPuzzleWhenDeadlineExpiresDuringNextCheck() {
        GeneratorProperties properties = new GeneratorProperties();
        AtomicLong clock = new AtomicLong();
        AtomicInteger checks = new AtomicInteger();
        PuzzleGenerator generator = new PuzzleGenerator(properties, (puzzle, solution, remaining) -> {
            if (checks.incrementAndGet() == 2) {
                clock.set(TimeUnit.SECONDS.toNanos(11));
            }
            return UniquenessChecker.Outcome.UNIQUE;
        }, clock::get, () -> new Random(1));
        Board puzzle = generator.generate(4);
        assertEquals(2, checks.get());
        assertEquals(1, countEmpty(puzzle));
    }

    private static int countEmpty(Board board) {
        int count = 0;
        for (int[] row : board.copyCells()) {
            for (int cell : row) {
                if (cell == 0) {
                    count++;
                }
            }
        }
        return count;
    }
}

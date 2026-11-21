package com.example.sudoku.generator;

import com.example.sudoku.domain.Board;
import com.example.sudoku.domain.BoxDimensions;
import com.example.sudoku.domain.SudokuValidator;
import com.example.sudoku.solver.SolverLimitException;
import com.example.sudoku.solver.UniquenessChecker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class PuzzleGenerator {
    @FunctionalInterface
    interface UniquenessCheck {
        UniquenessChecker.Outcome check(Board puzzle, Board solution, long timeoutMs);
    }

    private final GeneratorProperties properties;
    private final UniquenessCheck uniqueness;
    private final LongSupplier clock;
    private final Supplier<Random> random;

    @Autowired
    public PuzzleGenerator(GeneratorProperties properties, UniquenessChecker uniqueness) {
        this(properties, uniqueness::checkAgainstSolution, System::nanoTime, ThreadLocalRandom::current);
    }

    PuzzleGenerator(GeneratorProperties properties, UniquenessCheck uniqueness,
            LongSupplier clock, Supplier<Random> random) {
        this.properties = properties;
        this.uniqueness = uniqueness;
        this.clock = clock;
        this.random = random;
    }

    public Board generate(int size) {
        long start = clock.getAsLong();
        BoxDimensions boxes = BoxDimensions.forSize(size);
        Random rng = random.get();
        Board solution = completeBoard(size, boxes, rng);
        Board verified = null;
        int[][] cells = solution.copyCells();
        List<Integer> positions = range(size * size);
        Collections.shuffle(positions, rng);
        for (int position : positions) {
            long remaining = remainingMs(start);
            if (remaining <= 0) {
                break;
            }
            int row = position / size;
            int column = position % size;
            int clue = cells[row][column];
            cells[row][column] = 0;
            Board candidate = new Board(size, cells);
            UniquenessChecker.Outcome outcome = uniqueness.check(candidate, solution, remaining);
            if (remainingMs(start) <= 0 || outcome == UniquenessChecker.Outcome.INCONCLUSIVE) {
                cells[row][column] = clue;
                break;
            }
            if (outcome == UniquenessChecker.Outcome.UNIQUE) {
                verified = candidate;
            } else if (outcome == UniquenessChecker.Outcome.MULTIPLE) {
                cells[row][column] = clue;
            } else {
                throw new IllegalStateException("A generated puzzle contradicted its validated solution");
            }
        }
        if (verified == null) {
            throw new SolverLimitException("Generation limit reached before a unique puzzle was verified");
        }
        return verified;
    }

    private long remainingMs(long start) {
        long elapsed = clock.getAsLong() - start;
        long budget = TimeUnit.MILLISECONDS.toNanos(properties.getTimeoutMs());
        long remaining = budget - elapsed;
        return remaining <= 0 ? 0 : Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining));
    }

    private Board completeBoard(int size, BoxDimensions boxes, Random rng) {
        List<Integer> rows = shuffledUnits(size, boxes.rows(), rng);
        List<Integer> columns = shuffledUnits(size, boxes.columns(), rng);
        List<Integer> values = range(size);
        Collections.shuffle(values, rng);
        int[][] cells = new int[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int base = (rows.get(row) * boxes.columns() + rows.get(row) / boxes.rows()
                        + columns.get(column)) % size;
                cells[row][column] = values.get(base) + 1;
            }
        }
        Board board = new Board(size, cells);
        if (!SudokuValidator.isCompleteAndValid(board)) {
            throw new IllegalStateException("Generated complete board failed validation");
        }
        return board;
    }

    private static List<Integer> shuffledUnits(int size, int unitSize, Random rng) {
        List<Integer> units = range(size / unitSize);
        Collections.shuffle(units, rng);
        List<Integer> result = new ArrayList<>();
        for (int unit : units) {
            List<Integer> offsets = range(unitSize);
            Collections.shuffle(offsets, rng);
            for (int offset : offsets) {
                result.add(unit * unitSize + offset);
            }
        }
        return result;
    }

    private static List<Integer> range(int end) {
        List<Integer> values = new ArrayList<>(end);
        for (int i = 0; i < end; i++) {
            values.add(i);
        }
        return values;
    }
}

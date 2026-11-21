package com.example.sudoku.solver;

import com.example.sudoku.domain.Board;
import com.example.sudoku.domain.BoxDimensions;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.Relationship;

final class SudokuLpModel {
    record Candidate(int row, int column, int value) {
    }

    final Board board;
    final List<Candidate> candidates = new ArrayList<>();
    final List<LinearConstraint> constraints = new ArrayList<>();

    SudokuLpModel(Board board) {
        this.board = board;
        int size = board.size();
        BoxDimensions boxes = BoxDimensions.forSize(size);
        // Cell equalities and nonnegativity also impose the upper bound of one.
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (board.get(row, column) != 0) {
                    continue;
                }
                for (int value = 1; value <= size; value++) {
                    if (allowed(board, boxes, row, column, value)) {
                        candidates.add(new Candidate(row, column, value));
                    }
                }
            }
        }

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (board.get(row, column) == 0) {
                    int r = row;
                    int c = column;
                    addEquality(candidate -> candidate.row == r && candidate.column == c);
                }
            }
        }
        for (int unit = 0; unit < size; unit++) {
            for (int value = 1; value <= size; value++) {
                int u = unit;
                int v = value;
                if (!rowContains(board, unit, value)) {
                    addEquality(candidate -> candidate.row == u && candidate.value == v);
                }
                if (!columnContains(board, unit, value)) {
                    addEquality(candidate -> candidate.column == u && candidate.value == v);
                }
                int startRow = (unit / (size / boxes.columns())) * boxes.rows();
                int startColumn = (unit % (size / boxes.columns())) * boxes.columns();
                if (!regionContains(board, boxes, startRow, startColumn, value)) {
                    addEquality(candidate -> candidate.row >= startRow
                            && candidate.row < startRow + boxes.rows()
                            && candidate.column >= startColumn
                            && candidate.column < startColumn + boxes.columns()
                            && candidate.value == v);
                }
            }
        }
    }

    private interface CandidatePredicate {
        boolean matches(Candidate candidate);
    }

    private void addEquality(CandidatePredicate predicate) {
        double[] coefficients = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            if (predicate.matches(candidates.get(i))) {
                coefficients[i] = 1;
            }
        }
        constraints.add(new LinearConstraint(coefficients, Relationship.EQ, 1));
    }

    private static boolean allowed(Board board, BoxDimensions boxes, int row, int column, int value) {
        int startRow = row / boxes.rows() * boxes.rows();
        int startColumn = column / boxes.columns() * boxes.columns();
        return !rowContains(board, row, value)
                && !columnContains(board, column, value)
                && !regionContains(board, boxes, startRow, startColumn, value);
    }

    private static boolean rowContains(Board board, int row, int value) {
        for (int column = 0; column < board.size(); column++) {
            if (board.get(row, column) == value) {
                return true;
            }
        }
        return false;
    }

    private static boolean columnContains(Board board, int column, int value) {
        for (int row = 0; row < board.size(); row++) {
            if (board.get(row, column) == value) {
                return true;
            }
        }
        return false;
    }

    private static boolean regionContains(Board board, BoxDimensions boxes, int startRow, int startColumn, int value) {
        for (int row = startRow; row < startRow + boxes.rows(); row++) {
            for (int column = startColumn; column < startColumn + boxes.columns(); column++) {
                if (board.get(row, column) == value) {
                    return true;
                }
            }
        }
        return false;
    }

    LinearConstraint branch(int index, int value) {
        double[] coefficients = new double[candidates.size()];
        coefficients[index] = 1;
        return new LinearConstraint(coefficients, Relationship.EQ, value);
    }

    LinearConstraint exclude(Board solution) {
        double[] coefficients = new double[candidates.size()];
        int emptyCells = 0;
        for (int row = 0; row < board.size(); row++) {
            for (int column = 0; column < board.size(); column++) {
                if (board.get(row, column) == 0) {
                    emptyCells++;
                }
            }
        }
        for (int i = 0; i < candidates.size(); i++) {
            Candidate candidate = candidates.get(i);
            if (solution.get(candidate.row, candidate.column) == candidate.value) {
                coefficients[i] = 1;
            }
        }
        return new LinearConstraint(coefficients, Relationship.LEQ, emptyCells - 1);
    }

    Board decode(double[] point) {
        int[][] cells = board.copyCells();
        int[] counts = new int[board.size() * board.size()];
        for (int i = 0; i < candidates.size(); i++) {
            if (Math.abs(point[i] - 1) <= LpSudokuSolver.EPSILON) {
                Candidate candidate = candidates.get(i);
                cells[candidate.row][candidate.column] = candidate.value;
                counts[candidate.row * board.size() + candidate.column]++;
            }
        }
        for (int row = 0; row < board.size(); row++) {
            for (int column = 0; column < board.size(); column++) {
                if (board.get(row, column) == 0 && counts[row * board.size() + column] != 1) {
                    throw new IllegalStateException("LP solution does not assign exactly one value to every cell");
                }
            }
        }
        return new Board(board.size(), cells);
    }
}

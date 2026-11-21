package com.example.sudoku.solver;

import com.example.sudoku.domain.Board;
import com.example.sudoku.domain.SudokuValidator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.apache.commons.math3.exception.TooManyIterationsException;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

public final class LpSudokuSolver {
    static final double EPSILON = 1e-6;

    public record Result(Optional<Board> solution, int nodesExplored) {
    }

    public Result solve(Board board) {
        return solve(board, 10_000, 100_000);
    }

    Result solve(Board board, int maxNodes, int maxIterations) {
        return solve(board, maxNodes, maxIterations, null);
    }

    Result solve(Board board, int maxNodes, int maxIterations, Board excluded) {
        if (excluded != null) {
            SudokuValidator.validateClues(board, excluded);
            if (!SudokuValidator.isCompleteAndValid(excluded)) {
                throw new IllegalArgumentException("Excluded solution must be valid and complete");
            }
        }
        if (SudokuValidator.hasConflicts(board)) {
            return new Result(Optional.empty(), 0);
        }
        if (SudokuValidator.isCompleteAndValid(board)) {
            return new Result(excluded == null ? Optional.of(board) : Optional.empty(), 0);
        }
        SudokuLpModel model = new SudokuLpModel(board);
        if (excluded != null) {
            model.constraints.add(model.exclude(excluded));
        }
        if (model.candidates.isEmpty()) {
            return new Result(Optional.empty(), 0);
        }
        Deque<List<LinearConstraint>> nodes = new ArrayDeque<>();
        nodes.push(List.of());
        int explored = 0;
        while (!nodes.isEmpty()) {
            if (explored >= maxNodes) {
                throw new SolverLimitException("Solver node limit reached");
            }
            List<LinearConstraint> branches = nodes.pop();
            List<LinearConstraint> constraints = new ArrayList<>(model.constraints);
            constraints.addAll(branches);
            explored++;
            PointValuePair optimum;
            try {
                optimum = new SimplexSolver().optimize(
                        new MaxIter(maxIterations),
                        new LinearObjectiveFunction(new double[model.candidates.size()], 0),
                        new LinearConstraintSet(constraints),
                        GoalType.MINIMIZE,
                        new NonNegativeConstraint(true));
            } catch (NoFeasibleSolutionException infeasible) {
                continue;
            } catch (TooManyIterationsException limit) {
                throw new SolverLimitException("Solver iteration limit reached");
            }
            double[] point = optimum.getPoint();
            int fractional = mostFractional(point);
            if (fractional == -1) {
                Board solution = model.decode(point);
                SudokuValidator.validateClues(board, solution);
                if (!SudokuValidator.isCompleteAndValid(solution)) {
                    throw new IllegalStateException("LP solution failed independent Sudoku validation");
                }
                if (excluded != null && sameBoard(solution, excluded)) {
                    throw new IllegalStateException("LP solution violates the no-good constraint");
                }
                return new Result(Optional.of(solution), explored);
            }
            List<LinearConstraint> zero = new ArrayList<>(branches);
            zero.add(model.branch(fractional, 0));
            nodes.push(zero);
            List<LinearConstraint> one = new ArrayList<>(branches);
            one.add(model.branch(fractional, 1));
            nodes.push(one);
        }
        return new Result(Optional.empty(), explored);
    }

    private static boolean sameBoard(Board first, Board second) {
        for (int row = 0; row < first.size(); row++) {
            for (int column = 0; column < first.size(); column++) {
                if (first.get(row, column) != second.get(row, column)) {
                    return false;
                }
            }
        }
        return true;
    }

    static int mostFractional(double[] point) {
        int selected = -1;
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < point.length; i++) {
            double value = point[i];
            if (!Double.isFinite(value) || value < -EPSILON || value > 1 + EPSILON) {
                throw new IllegalStateException("LP returned a value outside binary bounds");
            }
            if (Math.abs(value) > EPSILON && Math.abs(value - 1) > EPSILON) {
                double distance = Math.abs(value - 0.5);
                if (distance < best) {
                    best = distance;
                    selected = i;
                }
            }
        }
        return selected;
    }
}

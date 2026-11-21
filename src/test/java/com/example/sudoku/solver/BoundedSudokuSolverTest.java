package com.example.sudoku.solver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.sudoku.domain.Board;
import com.example.sudoku.domain.SudokuValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoundedSudokuSolverTest {
    @Test
    void solvesInAWorkerAndEnforcesIterationAndNodeLimits() {
        Board hard = new Board(9, new int[][] {
                {1, 0, 0, 0, 0, 7, 0, 9, 0}, {0, 3, 0, 0, 2, 0, 0, 0, 8},
                {0, 0, 9, 6, 0, 0, 5, 0, 0}, {0, 0, 5, 3, 0, 0, 9, 0, 0},
                {0, 1, 0, 0, 8, 0, 0, 0, 2}, {6, 0, 0, 0, 0, 4, 0, 0, 0},
                {3, 0, 0, 0, 0, 0, 0, 1, 0}, {0, 4, 0, 0, 0, 0, 0, 0, 7},
                {0, 0, 7, 0, 0, 0, 3, 0, 0}
        });
        SolverProperties properties = new SolverProperties();
        BoundedSudokuSolver solver = new BoundedSudokuSolver(properties);
        assertTrue(SudokuValidator.isCompleteAndValid(solver.solve(hard).solution().orElseThrow()));
        properties.setMaxNodes(1);
        assertThrows(SolverLimitException.class, () -> solver.solve(hard));
        properties.setMaxNodes(10_000);
        properties.setMaxIterations(1);
        assertThrows(SolverLimitException.class, () -> solver.solve(hard));
    }

    @Test
    void timeoutTerminatesAndReapsTheWorker(@TempDir Path temp) throws Exception {
        Path marker = temp.resolve("worker-pid");
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        String java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();
        List<String> command = List.of(java, "-cp", System.getProperty("surefire.test.class.path"),
                SimplexWorker.class.getName(), marker.toString());
        assertThrows(SolverLimitException.class, () -> SolverProcess.execute(command, new byte[0], 2_000));
        long pid = Long.parseLong(Files.readString(marker));
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
    }

    public static class SimplexWorker {
        public static void main(String[] args) throws Exception {
            SudokuLpModel model = new SudokuLpModel(new Board(12, new int[12][12]));
            Files.writeString(Path.of(args[0]), Long.toString(ProcessHandle.current().pid()));
            while (true) {
                new SimplexSolver().optimize(new MaxIter(100_000),
                        new LinearObjectiveFunction(new double[model.candidates.size()], 0),
                        new LinearConstraintSet(model.constraints),
                        GoalType.MINIMIZE, new NonNegativeConstraint(true));
            }
        }
    }
}

package com.example.sudoku.solver;

import com.example.sudoku.domain.Board;
import com.example.sudoku.domain.SudokuValidator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public final class BoundedSudokuSolver {
    private final SolverProperties properties;

    public BoundedSudokuSolver(SolverProperties properties) {
        this.properties = properties;
    }

    public LpSudokuSolver.Result solve(Board board) {
        return solve(board, properties.getTimeoutMs());
    }

    public LpSudokuSolver.Result solve(Board board, long timeoutMs) {
        return solve(board, null, timeoutMs);
    }

    public LpSudokuSolver.Result solve(Board board, Board excluded, long timeoutMs) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(board.size());
            output.writeInt(properties.getMaxNodes());
            output.writeInt(properties.getMaxIterations());
            for (int[] row : board.copyCells()) {
                for (int value : row) {
                    output.writeInt(value);
                }
            }
            output.writeBoolean(excluded != null);
            if (excluded != null) {
                SudokuValidator.validateClues(board, excluded);
                if (!SudokuValidator.isCompleteAndValid(excluded)) {
                    throw new IllegalArgumentException("Excluded solution must be valid and complete");
                }
                for (int[] row : excluded.copyCells()) {
                    for (int value : row) {
                        output.writeInt(value);
                    }
                }
            }
            byte[] response = SolverProcess.execute(workerCommand(), bytes.toByteArray(),
                    Math.min(timeoutMs, properties.getTimeoutMs()));
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(response));
            String status = input.readUTF();
            if (status.equals("LIMIT")) {
                throw new SolverLimitException(input.readUTF());
            }
            int nodes = input.readInt();
            if (status.equals("INFEASIBLE")) {
                return new LpSudokuSolver.Result(Optional.empty(), nodes);
            }
            if (!status.equals("SOLVED")) {
                throw new IllegalStateException("Unexpected solver worker response: " + status);
            }
            int[][] cells = new int[board.size()][board.size()];
            for (int row = 0; row < board.size(); row++) {
                for (int column = 0; column < board.size(); column++) {
                    cells[row][column] = input.readInt();
                }
            }
            Board solution = new Board(board.size(), cells);
            SudokuValidator.validateClues(board, solution);
            if (!SudokuValidator.isCompleteAndValid(solution)) {
                throw new IllegalStateException("Solver worker returned an invalid solution");
            }
            return new LpSudokuSolver.Result(Optional.of(solution), nodes);
        } catch (IOException error) {
            throw new IllegalStateException("Invalid solver worker response", error);
        }
    }

    private List<String> workerCommand() {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        String java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();
        List<String> command = new ArrayList<>();
        command.add(java);
        String classpath = System.getProperty("java.class.path");
        if (getClass().getClassLoader().getClass().getName().equals("org.springframework.boot.loader.launch.LaunchedClassLoader")) {
            command.add("-jar");
            command.add(classpath);
            command.add("--sudoku-worker");
        } else {
            command.add("-cp");
            command.add(System.getProperty("surefire.test.class.path", classpath));
            command.add(SolverWorker.class.getName());
        }
        return command;
    }
}

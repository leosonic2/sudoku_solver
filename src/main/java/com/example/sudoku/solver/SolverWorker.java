package com.example.sudoku.solver;

import com.example.sudoku.domain.Board;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class SolverWorker {
    private SolverWorker() {
    }

    public static void main(String[] args) throws IOException {
        run();
    }

    public static void run() throws IOException {
        DataInputStream input = new DataInputStream(System.in);
        DataOutputStream output = new DataOutputStream(System.out);
        int size = input.readInt();
        int maxNodes = input.readInt();
        int maxIterations = input.readInt();
        int[][] cells = new int[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                cells[row][column] = input.readInt();
            }
        }
        Board excluded = null;
        if (input.readBoolean()) {
            int[][] excludedCells = new int[size][size];
            for (int row = 0; row < size; row++) {
                for (int column = 0; column < size; column++) {
                    excludedCells[row][column] = input.readInt();
                }
            }
            excluded = new Board(size, excludedCells);
        }
        try {
            LpSudokuSolver.Result result = new LpSudokuSolver().solve(
                    new Board(size, cells), maxNodes, maxIterations, excluded);
            output.writeUTF(result.solution().isPresent() ? "SOLVED" : "INFEASIBLE");
            output.writeInt(result.nodesExplored());
            if (result.solution().isPresent()) {
                Board solution = result.solution().orElseThrow();
                for (int row = 0; row < size; row++) {
                    for (int column = 0; column < size; column++) {
                        output.writeInt(solution.get(row, column));
                    }
                }
            }
        } catch (SolverLimitException limit) {
            output.writeUTF("LIMIT");
            output.writeUTF(limit.getMessage());
        }
        output.flush();
    }
}

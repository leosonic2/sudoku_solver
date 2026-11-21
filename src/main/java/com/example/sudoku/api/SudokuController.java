package com.example.sudoku.api;

import com.example.sudoku.domain.Board;
import com.example.sudoku.domain.BoxDimensions;
import com.example.sudoku.domain.SudokuValidator;
import com.example.sudoku.generator.PuzzleGenerator;
import com.example.sudoku.solver.BoundedSudokuSolver;
import com.example.sudoku.solver.LpSudokuSolver;
import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sudoku")
public final class SudokuController {
    public record GenerateRequest(Integer size) {
    }

    public record GenerateResponse(int size, int boxRows, int boxCols, int[][] puzzle) {
    }

    public record SolveRequest(Integer size, Integer boxRows, Integer boxCols,
                               Integer[][] puzzle, Integer[][] current) {
    }

    public record SolveResponse(int[][] solution, long solveTimeMs, int nodesExplored) {
    }

    private final PuzzleGenerator generator;
    private final BoundedSudokuSolver solver;

    public SudokuController(PuzzleGenerator generator, BoundedSudokuSolver solver) {
        this.generator = generator;
        this.solver = solver;
    }

    @PostMapping("/generate")
    public GenerateResponse generate(@RequestBody GenerateRequest request) {
        int size = required(request.size(), "size");
        BoxDimensions boxes = BoxDimensions.forSize(size);
        Board puzzle = generator.generate(size);
        return new GenerateResponse(size, boxes.rows(), boxes.columns(), puzzle.copyCells());
    }

    @PostMapping("/solve")
    public SolveResponse solve(@RequestBody SolveRequest request) {
        int size = required(request.size(), "size");
        int boxRows = required(request.boxRows(), "boxRows");
        int boxCols = required(request.boxCols(), "boxCols");
        BoxDimensions.validate(size, boxRows, boxCols);
        Board puzzle = board(size, request.puzzle());
        Board current = board(size, request.current());
        SudokuValidator.validateClues(puzzle, current);
        long start = System.nanoTime();
        LpSudokuSolver.Result result = solver.solve(current);
        Board solution = result.solution().orElseThrow(InfeasiblePuzzleException::new);
        return new SolveResponse(solution.copyCells(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), result.nodesExplored());
    }

    private static int required(Integer value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required and must be an integer");
        }
        return value;
    }

    private static Board board(int size, Integer[][] input) {
        if (input == null || input.length != size) {
            throw new IllegalArgumentException("Board must have exactly " + size + " rows");
        }
        int[][] cells = new int[size][size];
        for (int row = 0; row < size; row++) {
            if (input[row] == null || input[row].length != size) {
                throw new IllegalArgumentException("Each board row must have exactly " + size + " cells");
            }
            for (int column = 0; column < size; column++) {
                cells[row][column] = required(input[row][column], "Board value");
            }
        }
        return new Board(size, cells);
    }
}

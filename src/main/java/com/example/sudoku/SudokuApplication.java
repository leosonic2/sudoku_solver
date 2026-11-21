package com.example.sudoku;

import com.example.sudoku.solver.SolverWorker;
import java.io.IOException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SudokuApplication {
    public static void main(String[] args) throws IOException {
        if (args.length == 1 && args[0].equals("--sudoku-worker")) {
            SolverWorker.run();
            return;
        }
        SpringApplication.run(SudokuApplication.class, args);
    }
}

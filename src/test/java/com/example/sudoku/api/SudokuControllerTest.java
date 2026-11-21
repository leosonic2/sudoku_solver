package com.example.sudoku.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sudoku.generator.GeneratorProperties;
import com.example.sudoku.solver.SolverProperties;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SudokuControllerTest {
    private static final String PUZZLE = "[[1,0,0,4],[0,4,1,0],[0,1,4,0],[4,0,0,1]]";
    private static final String SOLVE = "{\"size\":4,\"boxRows\":2,\"boxCols\":2,\"puzzle\":%s,\"current\":%s}";

    @Autowired
    MockMvc mvc;

    @Autowired
    SolverProperties solverProperties;

    @Autowired
    GeneratorProperties generatorProperties;

    @Test
    void generatesAndSolvesWithMetadata() throws Exception {
        mvc.perform(post("/api/sudoku/generate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"size\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(4))
                .andExpect(jsonPath("$.boxRows").value(2))
                .andExpect(jsonPath("$.boxCols").value(2))
                .andExpect(jsonPath("$.puzzle.length()").value(4))
                .andExpect(jsonPath("$.solution").doesNotExist());
        mvc.perform(post("/api/sudoku/solve").contentType(MediaType.APPLICATION_JSON)
                        .content(SOLVE.formatted(PUZZLE, PUZZLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solution[0][0]").value(1))
                .andExpect(jsonPath("$.solveTimeMs", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.nodesExplored", greaterThanOrEqualTo(0)));
    }

    @Test
    void rejectsMalformedInputAndClueTampering() throws Exception {
        for (String request : new String[] {
                "{\"size\":17}", "{\"size\":4.5}", "{\"size\":\"4\"}", "{\"size\":null}", "{"
        }) {
            mvc.perform(post("/api/sudoku/generate").contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").isString());
        }
        String tampered = "[[0,0,0,4],[0,4,1,0],[0,1,4,0],[4,0,0,1]]";
        String nullCell = "[[null,0,0,4],[0,4,1,0],[0,1,4,0],[4,0,0,1]]";
        for (String request : new String[] {
                SOLVE.formatted(PUZZLE, tampered),
                SOLVE.formatted(PUZZLE, nullCell),
                SOLVE.formatted(PUZZLE, "[[1]]"),
                SOLVE.formatted(PUZZLE, PUZZLE).replace("\"boxRows\":2", "\"boxRows\":4"),
                SOLVE.formatted(PUZZLE, PUZZLE).replace("[1,0,0,4]", "[1.2,0,0,4]"),
                SOLVE.formatted(PUZZLE, PUZZLE).replace("[1,0,0,4]", "[5,0,0,4]")
        }) {
            mvc.perform(post("/api/sudoku/solve").contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
    }

    @Test
    void returnsInfeasibleAndLimitErrors() throws Exception {
        String conflicting = "[[1,1,0,4],[0,4,1,0],[0,1,4,0],[4,0,0,1]]";
        mvc.perform(post("/api/sudoku/solve").contentType(MediaType.APPLICATION_JSON)
                        .content(SOLVE.formatted(PUZZLE, conflicting)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INFEASIBLE"));

        int oldIterations = solverProperties.getMaxIterations();
        long oldGenerationTimeout = generatorProperties.getTimeoutMs();
        try {
            solverProperties.setMaxIterations(1);
            mvc.perform(post("/api/sudoku/solve").contentType(MediaType.APPLICATION_JSON)
                            .content(SOLVE.formatted(PUZZLE, PUZZLE)))
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code").value("LIMIT_REACHED"));
            generatorProperties.setTimeoutMs(1);
            mvc.perform(post("/api/sudoku/generate").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"size\":4}"))
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code").value("LIMIT_REACHED"));
        } finally {
            solverProperties.setMaxIterations(oldIterations);
            generatorProperties.setTimeoutMs(oldGenerationTimeout);
        }
    }

    @Test
    void concurrentRequestsKeepTheirBoardsSeparate() throws Exception {
        String other = "[[2,0,0,3],[0,3,2,0],[0,2,3,0],[3,0,0,2]]";
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> mvc.perform(post("/api/sudoku/solve")
                            .contentType(MediaType.APPLICATION_JSON).content(SOLVE.formatted(PUZZLE, PUZZLE)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.solution[0][0]").value(1)));
            var second = executor.submit(() -> mvc.perform(post("/api/sudoku/solve")
                            .contentType(MediaType.APPLICATION_JSON).content(SOLVE.formatted(other, other)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.solution[0][0]").value(2)));
            first.get();
            second.get();
        }
    }
}

package com.example.sudoku.solver;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sudoku.solver")
public class SolverProperties {
    private long timeoutMs = 30_000;
    private int maxNodes = 10_000;
    private int maxIterations = 100_000;

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Solver timeout must be positive");
        }
        this.timeoutMs = timeoutMs;
    }

    public int getMaxNodes() {
        return maxNodes;
    }

    public void setMaxNodes(int maxNodes) {
        if (maxNodes <= 0) {
            throw new IllegalArgumentException("Solver node limit must be positive");
        }
        this.maxNodes = maxNodes;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("Solver iteration limit must be positive");
        }
        this.maxIterations = maxIterations;
    }
}

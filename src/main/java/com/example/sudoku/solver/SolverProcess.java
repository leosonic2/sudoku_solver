package com.example.sudoku.solver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

final class SolverProcess {
    private SolverProcess() {
    }

    static byte[] execute(List<String> command, byte[] request, long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new SolverLimitException("Solver time limit reached");
        }
        long start = System.nanoTime();
        Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException error) {
            throw new IllegalStateException("Unable to start solver worker", error);
        }
        try (ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<byte[]> stdout = readers.submit(() -> process.getInputStream().readAllBytes());
            Future<byte[]> stderr = readers.submit(() -> process.getErrorStream().readAllBytes());
            try {
                try (var input = process.getOutputStream()) {
                    input.write(request);
                }
                long remaining = TimeUnit.MILLISECONDS.toNanos(timeoutMs) - (System.nanoTime() - start);
                if (remaining <= 0 || !process.waitFor(remaining, TimeUnit.NANOSECONDS)) {
                    process.destroyForcibly();
                    process.waitFor();
                    throw new SolverLimitException("Solver time limit reached");
                }
                byte[] errors = stderr.get();
                if (process.exitValue() != 0) {
                    throw new IllegalStateException("Solver worker failed: " + new String(errors, StandardCharsets.UTF_8));
                }
                return stdout.get();
            } catch (IOException | ExecutionException error) {
                throw new IllegalStateException("Solver worker communication failed", error);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Solver worker interrupted", interrupted);
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }
    }
}

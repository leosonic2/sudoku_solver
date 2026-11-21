package com.example.sudoku.generator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sudoku.generator")
public class GeneratorProperties {
    private long timeoutMs = 10_000;

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Generator timeout must be positive");
        }
        this.timeoutMs = timeoutMs;
    }
}

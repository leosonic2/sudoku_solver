package com.example.sudoku.api;

import com.example.sudoku.solver.SolverLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record ErrorResponse(String code, String message) {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalid(IllegalArgumentException error) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", error.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> malformed(HttpMessageNotReadableException error) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request must contain valid JSON with integer values");
    }

    @ExceptionHandler(InfeasiblePuzzleException.class)
    ResponseEntity<ErrorResponse> infeasible(InfeasiblePuzzleException error) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "INFEASIBLE", error.getMessage());
    }

    @ExceptionHandler(SolverLimitException.class)
    ResponseEntity<ErrorResponse> limit(SolverLimitException error) {
        return response(HttpStatus.GATEWAY_TIMEOUT, "LIMIT_REACHED", error.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception error) {
        if (error instanceof org.springframework.web.ErrorResponse webError) {
            return ResponseEntity.status(webError.getStatusCode())
                    .body(new ErrorResponse("INVALID_REQUEST", webError.getBody().getDetail()));
        }
        LOG.error("Sudoku request failed", error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "The request could not be completed");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}

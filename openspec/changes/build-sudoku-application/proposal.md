# Proposal

## Why

The project currently contains a product specification but no playable application. Users need a way to generate and play variable-size Sudoku puzzles and obtain a mathematically verified automatic solution.

## What Changes

- Create a single Java 25 Spring Boot application containing a Vue 3 frontend, packaged and served together.
- Generate uniquely solvable N×N puzzles for user-selected sizes from 2 through 16, with deterministic rectangular region dimensions.
- Support accessible manual play, conflict feedback, reset, and completion detection.
- Solve puzzles and player entries using an Apache Commons Math linear programming model with integral branch-and-bound, handling infeasibility and resource limits explicitly.
- Expose generation and solving through a stateless REST API and provide automated backend, frontend, and packaging tests.

## Capabilities

### New Capabilities

- `sudoku-generation`: Board dimensions, regions, puzzle generation, uniqueness, and generation errors.
- `sudoku-gameplay`: Interactive board, editing, validation feedback, reset, and automatic solve interaction.
- `sudoku-solving`: LP-based integral solving, solve API contract, input validation, and error handling.
- `application-delivery`: Java 25 Spring Boot and embedded Vue build, deployment, and same-origin serving.

### Modified Capabilities

None.

## Impact

- Adds a Maven Spring Boot backend, Vue/Vite source under `src/main/frontend`, and their tests to an otherwise greenfield project.
- Adds Apache Commons Math, Spring Boot, frontend build tooling, and a single runnable application artifact.
- Introduces `POST /api/sudoku/generate` and `POST /api/sudoku/solve`; no existing API or persistence is affected.

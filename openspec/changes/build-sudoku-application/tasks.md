# Tasks

## 1. Application foundation

- [ ] 1.1 Scaffold the Maven Spring Boot application and wrapper for Java 25 with a compatible Spring Boot 3.x release and Commons Math 3.6.1; verify a Java 25 compile succeeds.
- [ ] 1.2 Create the Vue 3/TypeScript/Vite project in `src/main/frontend`, integrate pinned Node/npm, frontend tests, and Vite asset generation into Maven, and configure the dev proxy; verify `mvn package` and `mvn spring-boot:run` build and serve the frontend and `-DskipTests` only skips tests.
- [ ] 1.3 Document Java 25 prerequisites, build, single-jar run, optional Vite development, and configurable limits; verify the commands match the project configuration.

## 2. Sudoku domain and solver

- [ ] 2.1 Implement board, region dimension, shape, value, clue, conflict, and completed-board validation; verify unit tests cover square, rectangular, prime, malformed, and contradictory boards.
- [ ] 2.2 Build the Apache Commons Math LP relaxation and binary branch-and-bound solver with fixed entries and independent solution validation; verify known 4-by-4, 6-by-6, easy and hard 9-by-9 puzzles, fractional branching, and infeasible boards.
- [ ] 2.3 Implement uniqueness checking with a no-good constraint and distinct unique, multiple, infeasible, and inconclusive outcomes; verify tests for each, including timeout during the second search.
- [ ] 2.4 Implement configurable node, iteration, and wall-clock budgets with cancellation that actually stops in-progress simplex work and cleans up resources; verify limit responses and cleanup using deterministic tests.

## 3. Puzzle generation and API

- [ ] 3.1 Generate randomized valid complete boards and remove clues only after a proven uniqueness check, retaining the last verified puzzle under a shared deadline; verify generated 4-by-4, 6-by-6 and 9-by-9 puzzles are unique and have empty cells, and test deadline paths before and after a verified removal.
- [ ] 3.2 Implement `POST /api/sudoku/generate` and `POST /api/sudoku/solve`, request validation, structured errors, solution metadata, and isolated request state; verify MockMvc tests cover successful responses, malformed inputs and clue tampering (400), infeasibility (422), limits (504), and concurrent requests.
- [ ] 3.3 Document request and response fields, status codes, board and region rules, and the generation and solver budget behavior; verify examples correspond to controller tests.

## 4. Vue game

- [ ] 4.1 Implement size selection, generation, prime-size notice, and confirmation before replacing a game; verify component tests for valid and invalid sizes, errors, and cancellation.
- [ ] 4.2 Implement a responsive and accessible grid with immutable clues, keyboard and number-pad input including multi-digit values, navigation, highlights, conflicts, reset, and completion detection; verify focused component and composable tests for each behavior.
- [ ] 4.3 Implement automatic solve submission, busy controls, solution styling, status, timing, and error preservation; verify frontend tests cover success, 422, timeout, and requests in flight.

## 5. End-to-end verification

- [ ] 5.1 Run backend and frontend tests and a clean Java 25 Maven package; launch the packaged jar and verify the same origin serves the UI and both API endpoints.
- [ ] 5.2 Exercise generating, playing, resetting, and automatically solving through the packaged UI, including infeasible entries and a prime or rectangular board; verify the board and errors match the specifications.
- [ ] 5.3 Measure representative 9-by-9 solves against the typical under-two-second target and verify a 16-by-16 solve obeys the 30-second limit; record sample, hardware, timing, timeout behavior, and any unmet target.

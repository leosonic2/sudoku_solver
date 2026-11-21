# Tasks

## 1. Application foundation

- [x] 1.1 Scaffold the Maven Spring Boot application and wrapper for Java 25 with a compatible Spring Boot 3.x release and Commons Math 3.6.1; verify a Java 25 compile succeeds.
- [x] 1.2 Create the Vue 3/TypeScript/Vite project in `src/main/frontend`, integrate pinned Node/npm, frontend tests, and Vite asset generation into Maven, and configure the dev proxy; verify `mvn package` and `mvn spring-boot:run` build and serve the frontend and `-DskipTests` only skips tests.
- [x] 1.3 Document Java 25 prerequisites, build, single-jar run, optional Vite development, and configurable limits; verify the commands match the project configuration.

## 2. Sudoku domain and solver

- [x] 2.1 Implement board, region dimension, shape, value, clue, conflict, and completed-board validation; verify unit tests cover square, rectangular, prime, malformed, and contradictory boards.
- [x] 2.2 Build the Apache Commons Math LP relaxation and binary branch-and-bound solver with fixed entries and independent solution validation; verify known 4-by-4, 6-by-6, easy and hard 9-by-9 puzzles, fractional branching, and infeasible boards.
- [x] 2.3 Implement uniqueness checking with a no-good constraint and distinct unique, multiple, infeasible, and inconclusive outcomes; verify tests for each, including timeout during the second search.
- [x] 2.4 Implement configurable node, iteration, and wall-clock budgets with cancellation that actually stops in-progress simplex work and cleans up resources; verify limit responses and cleanup using deterministic tests.

## 3. Puzzle generation and API

- [x] 3.1 Generate randomized valid complete boards and remove clues only after a proven uniqueness check, retaining the last verified puzzle under a shared deadline; verify generated 4-by-4, 6-by-6 and 9-by-9 puzzles are unique and have empty cells, and test deadline paths before and after a verified removal.
- [x] 3.2 Implement `POST /api/sudoku/generate` and `POST /api/sudoku/solve`, request validation, structured errors, solution metadata, and isolated request state; verify MockMvc tests cover successful responses, malformed inputs and clue tampering (400), infeasibility (422), limits (504), and concurrent requests.
- [x] 3.3 Document request and response fields, status codes, board and region rules, and the generation and solver budget behavior; verify examples correspond to controller tests.

## 4. Vue game

- [x] 4.1 Implement size selection, generation, prime-size notice, and confirmation before replacing a game; verify component tests for valid and invalid sizes, errors, and cancellation.
- [x] 4.2 Implement a responsive and accessible grid with immutable clues, keyboard and number-pad input including multi-digit values, navigation, highlights, conflicts, reset, and completion detection; verify focused component and composable tests for each behavior.
- [x] 4.3 Implement automatic solve submission, busy controls, solution styling, status, timing, and error preservation; verify frontend tests cover success, 422, timeout, and requests in flight.

## 5. End-to-end verification

- [x] 5.1 Run backend and frontend tests and a clean Java 25 Maven package; verify the running application serves the UI and both API endpoints from the same origin.
  - On 2026-11-21, a clean Java 25 package passed 21 backend and 10 frontend tests; the 10 frontend tests were run again against the current source. The running application served the UI and both APIs on `http://127.0.0.1:8080`.
  - Per user direction, the packaged JAR was not launched or verified. The running process used `SudokuApplication` rather than a verified packaged-JAR launch; the packaged-delivery requirement remains unverified.
- [x] 5.2 Exercise generating, playing, resetting, and automatically solving through the running UI, including infeasible entries and a prime or rectangular board; verify the board and errors match the specifications.
  - On 2026-11-21, 6-by-6 and 5-by-5 games were generated and solved with independently validated rows, columns, and regions. Conflict feedback, the 422 error and board preservation, reset, clue immutability, keyboard navigation, and replacement confirmation were exercised. Packaged-JAR UI behavior was not verified.
- [x] 5.3 Measure representative 9-by-9 solves against the typical under-two-second target and 16-by-16 solves against the 30-second budget; record sample, hardware, timing, timeout behavior, and any unmet target.
  - Environment: Windows 11 Pro, Intel Core Ultra 7 265K (20 cores, 20 logical processors), approximately 63.35 GiB usable memory; Java 25.0.5.1 was used for the clean build. Measurements used the already-running application.
  - 9-by-9 samples below use `/` between rows and `0` for empty cells. All returned valid solutions preserving clues. Server times were 135 ms, 137 ms, and 1,959 ms respectively; median 137 ms, mean 744 ms.
    - Easy, 30 clues: `530070000/600195000/098000060/800060003/400803001/700020006/060000280/000419005/000080079`.
    - Medium, 32 clues: `003020600/900305001/001806400/008102900/700000008/006708200/002609500/800203009/005010300`.
    - Hard, 23 clues: `100007090/030020008/009600500/005300900/010080002/600004000/300000010/040000007/007000300`.
  - A 16-by-16 board with 128 clues returned a valid solution in 173 ms server time (198 ms round trip). For zero-based `r` and `c`, its clues were `((r * 4 + floor(r / 4) + c) % 16) + 1` where `(r + c) % 2 == 0`, and zero elsewhere.
  - An empty 16-by-16 board returned HTTP 504 `LIMIT_REACHED` after 30,048.1 ms round trip; a repeat returned 504 after 30,051.8 ms, with the worker observed terminated by 30,056.4 ms after its creation, including polling delay. The strict 30,000 ms end-to-end target was exceeded by 48.1 ms and 51.8 ms respectively; these measurements do not establish the exact internal termination time.

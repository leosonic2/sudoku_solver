# Sudoku N×N — Generator, Player and Linear Programming Solver

## 1. Overview
A web application built as a **single Java Spring Boot project** that also contains the **Vue 3** frontend (source in `src/main/frontend`, built into `src/main/resources/static` and served by Spring Boot). Users choose a size **N**, generate an N×N Sudoku puzzle, play it manually, and press **Solve Automatically** to have the backend solve it with **linear programming** using the **Apache Commons Math** library (`org.apache.commons:commons-math3`).

Reference: Pedro Albuquerque, "Resolvendo Sudoku por meio de programação linear" (binary formulation x_ijk), available in this folder.

## 2. Technology Stack
| Layer | Technology |
|---|---|
| Backend | Java 25, a Spring Boot 3.x release compatible with Java 25 (spring-boot-starter-web, spring-boot-starter-validation), Maven |
| Optimization | Apache Commons Math 3.6.1 (`org.apache.commons.math3.optim.linear.SimplexSolver`) |
| Frontend | Vue 3 (Composition API), TypeScript, Vite, Axios or fetch |
| Tests | JUnit 5, Spring Boot Test, MockMvc; Vitest + Vue Test Utils |
| Persistence | None (stateless; game state lives in the browser) |

## 3. Domain Rules
### 3.1 Board size
- N is an integer chosen by the user; allowed range **2 ≤ N ≤ 16** (default 9). The upper limit is a performance limit (the model has N³ variables).
- Values are integers 1..N. An empty cell is represented as `0` in all API payloads.

### 3.2 Regions (boxes)
- Let `boxRows` = the largest divisor of N that is ≤ √N, and `boxCols` = N / boxRows.
- Examples: 4 → 2×2, 6 → 2×3, 8 → 2×4, 9 → 3×3, 12 → 3×4, 16 → 4×4.
- If N is prime, boxRows = 1 (regions equal rows, so the game becomes a Latin square). The UI must display a notice in this case.

### 3.3 Validity
A completed board is valid when every row, column and region contains each value 1..N exactly once and all original clues are unchanged.

## 4. Functional Requirements
### FR-1 Generate puzzle
- UI: a numeric input for N plus a **Generate** button. N is validated on the frontend and the backend (HTTP 400 on an invalid N).
- Backend algorithm:
  1. Build a complete valid board: start from a randomized pattern-based base grid, then apply random row/column permutations within bands/stacks, band/stack permutations and symbol permutations. (Alternative: solve an empty board with the LP solver after placing a few random seed clues.)
  2. Remove cells in random order. After each removal, check uniqueness with the solver (the search stops after 2 solutions). Restore the cell if uniqueness is lost.
  3. Stop when no further cell can be removed or a time budget is reached (configurable, default 10 s). The puzzle always has at least one empty cell.
- The response contains the puzzle and region dimensions; the solution is **not** sent.

### FR-2 Play
- Render an N×N grid with thicker borders between regions.
- Clues are read-only and visually distinct. Editable cells accept 1..N via keyboard (multi-digit input for N > 9) or an on-screen number pad, and can be cleared (Backspace/Delete/0).
- Arrow-key navigation; highlight the selected cell and its row, column and region.
- Real-time conflict highlighting (duplicate values in a row, column or region).
- **Reset** button clears user entries and keeps the clues.
- Completion is detected automatically: when the board is full and valid, show "Puzzle solved!".
- Optional timer and move counter.
- Ask for confirmation before discarding a game in progress when generating a new one.

### FR-3 Solve Automatically
- The **Solve Automatically** button sends the original puzzle plus the current user entries to the backend.
- User entries are treated as fixed constraints. If they make the puzzle infeasible, the backend returns HTTP 422 and the UI shows "Your current entries make the puzzle unsolvable", leaving the board unchanged. (The user may press Reset and retry.)
- On success the grid is filled; solver-filled cells are styled differently and the game is marked "solved automatically".
- While solving: a loading indicator is shown and the Generate/Solve/Reset buttons are disabled.
- The UI displays the solve time returned by the backend.

## 5. Linear Programming Model
### 5.1 Variables
x[i][j][k] ∈ {0,1}, with i, j, k ∈ {1..N}: x[i][j][k] = 1 if cell (i, j) contains value k. Total: N³ variables. Index mapping (0-based): `idx = (i·N + j)·N + k`.

### 5.2 Objective
This is a feasibility problem: minimize 0 (a `LinearObjectiveFunction` with all coefficients 0). Any feasible integral solution is a valid Sudoku solution.

### 5.3 Constraints
1. One value per cell: Σ_k x[i][j][k] = 1 for all i, j
2. Each value once per row: Σ_j x[i][j][k] = 1 for all i, k
3. Each value once per column: Σ_i x[i][j][k] = 1 for all j, k
4. Each value once per region: Σ_{(i,j)∈B} x[i][j][k] = 1 for all regions B and values k
5. Givens: x[i][j][v] = 1 for every clue or user entry v at (i, j)
6. Bounds: 0 ≤ x ≤ 1 (via `NonNegativeConstraint(true)` plus x ≤ 1 constraints)

Recommended preprocessing: variables forced to 0 by the givens (same cell, row, column or region) are removed or fixed, which greatly reduces the model size.

### 5.4 Integrality with Apache Commons Math
`SimplexSolver` solves continuous LPs only. To guarantee a binary solution, the solver implements **branch-and-bound**:
1. Solve the LP relaxation with `SimplexSolver.optimize(objective, LinearConstraintSet, GoalType.MINIMIZE, new NonNegativeConstraint(true), new MaxIter(...))`.
2. If `NoFeasibleSolutionException` is thrown, prune the node.
3. If all variables are within ε = 1e-6 of 0 or 1, an integral solution was found: round, decode and validate it.
4. Otherwise choose the most fractional variable (closest to 0.5) and branch on x = 1 (explored first) and x = 0, recursing depth-first.
5. Limits: maximum nodes and timeout (configurable, defaults 10,000 nodes / 30 s). Exceeding them throws `SolverTimeoutException`, mapped to HTTP 504.

- For uniqueness checks, the search continues after the first solution by adding a "no-good" cut Σ_{x*=1} x ≤ N² − 1 and stops at the second solution.
- Every returned solution is validated independently by `SudokuValidator` before being sent to the client.

### 5.5 Decoding
cell(i, j) = the k for which x[i][j][k] ≈ 1.

## 6. REST API (base path `/api/sudoku`)
### POST /generate
Request:
```json
{ "size": 9 }
```
Response 200:
```json
{ "size": 9, "boxRows": 3, "boxCols": 3, "puzzle": [[5, 3, 0, ...], ...] }
```
Errors: 400 (invalid size), 504 (generation timeout).

### POST /solve
Request:
```json
{ "size": 9, "boxRows": 3, "boxCols": 3, "puzzle": [[...]], "current": [[...]] }
```
Response 200:
```json
{ "solution": [[...]], "solveTimeMs": 152, "nodesExplored": 3 }
```
Errors: 400 (malformed board, value out of range, `current` changes a clue, wrong dimensions), 422 (infeasible or conflicting entries), 504 (solver limit reached).

### POST /validate (optional)
Request: `{ "size", "boxRows", "boxCols", "board" }`
Response: `{ "valid": true, "complete": false, "conflicts": [{ "row": 0, "col": 1 }] }`

### Error body (all errors)
```json
{ "code": "INFEASIBLE", "message": "..." }
```

## 7. Project Structure (single Spring Boot project)
The Vue frontend lives **inside** the Spring Boot project. There is one Maven project, one build and one deployable jar; no separate frontend repository or server is required.

```
sudoku-solver/                      (Spring Boot project root)
├── pom.xml
├── mvnw, mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/com/example/sudoku/...     (backend, see 7.1)
│   │   ├── resources/
│   │   │   ├── application.yml
│   │   │   └── static/                     (generated: Vue build output, not committed)
│   │   └── frontend/                       (Vue 3 + Vite source, see section 8)
│   │       ├── package.json
│   │       ├── vite.config.ts
│   │       ├── index.html
│   │       └── src/...
│   └── test/java/com/example/sudoku/...
```

- `vite.config.ts` sets `build.outDir` to `../resources/static` (with `emptyOutDir: true`), so the built frontend is packaged in the jar and served by Spring Boot at `/`.
- The Maven build uses `com.github.eirslett:frontend-maven-plugin` to install Node/npm locally and run `npm install`, `npm run test` (unless `-DskipTests`) and `npm run build` during `generate-resources`. `mvn package` therefore produces a single runnable jar containing both backend and frontend.
- The project targets Java 25: configure the Maven compiler release to `25`, and use a Java 25 JDK to build and Java 25 runtime to run the application.
- `src/main/resources/static/`, `src/main/frontend/node_modules/` and the local `node/` folder are listed in `.gitignore`.
- The frontend calls the API with relative URLs (`/api/sudoku/...`); since both are served from the same origin, no CORS configuration is needed in production.
- A Spring controller forwards non-API, non-asset routes to `index.html` (SPA fallback) if client-side routing is used.

### 7.1 Backend Packages
```
com.example.sudoku
├── SudokuApplication
├── api/        SudokuController, dto/ (GenerateRequest, SolveRequest, PuzzleResponse,
│               SolveResponse, ErrorResponse), GlobalExceptionHandler
├── domain/     Board, BoxDimensions, SudokuValidator
├── solver/     SudokuSolver (interface), SudokuLpModelBuilder, BranchAndBoundSolver,
│               LpSudokuSolver, SolverProperties
└── generator/  PuzzleGenerator
```
- `application.yml` properties: `sudoku.max-size`, `sudoku.solver.timeout-ms`, `sudoku.solver.max-nodes`, `sudoku.generator.timeout-ms`.
- The frontend build is served as static content from `src/main/resources/static` by the same Spring Boot application. During development, the Vite dev server proxies `/api` to the backend, so no CORS configuration is required.

## 8. Frontend Architecture
Located at `src/main/frontend` inside the Spring Boot project.
```
src/main/frontend/src
├── App.vue
├── components/   SizeSelector.vue, SudokuBoard.vue, SudokuCell.vue, NumberPad.vue,
│                 GameControls.vue, StatusBar.vue
├── composables/  useSudokuGame.ts (state, conflicts, completion)
├── api/          sudokuApi.ts
└── types/        sudoku.ts
```
- State: `size`, `boxRows`, `boxCols`, `puzzle`, `board`, `selectedCell`, `conflicts`, `status` (`idle | playing | solving | solved | solvedAutomatically | error`).
- The grid uses CSS grid with `grid-template-columns: repeat(N, 1fr)` and a font size scaled to N.

## 9. Non-Functional Requirements
- 9×9 solve: typically < 2 s; 16×16: < 30 s (bounded by the timeout).
- Stateless backend; concurrent requests are safe (no shared mutable solver state).
- Responsive layout (desktop and mobile); keyboard accessible; ARIA labels on cells.
- UI text in English.

## 10. Testing
- Unit tests: box dimension calculation; model builder (N³ variables, 4N² equality constraints plus givens); validator; branch-and-bound on known puzzles (4×4, 6×6, 9×9 easy and hard); infeasible puzzle → exception; uniqueness detection on a puzzle with multiple solutions.
- Generator tests: for N ∈ {4, 6, 9}, the generated puzzle is valid, has a unique solution and keeps at least one empty cell.
- Controller tests (MockMvc): 200, 400 and 422 paths.
- Frontend tests: conflict highlighting, clue immutability, reset, Solve button disabled while loading, applying the solution.

## 11. Build & Run
- Full build (backend + frontend): `mvn package` → `target/sudoku-solver.jar`; run with `java -jar target/sudoku-solver.jar` and open `http://localhost:8080`.
- Run from source: `mvn spring-boot:run` (the frontend is built by Maven and served at `http://localhost:8080`).
- Optional hot-reload development: run `mvn spring-boot:run`, then in `src/main/frontend` run `npm run dev` (port 5173, Vite proxy `/api` → `http://localhost:8080`).

## 12. Acceptance Criteria
1. The user can enter N (2–16), generate a puzzle, and see an N×N board with correct regions.
2. The user can fill and clear cells; clues cannot be edited; conflicts are highlighted; completion is detected.
3. Solve Automatically fills the board with a valid solution computed by the Apache Commons Math LP model plus branch-and-bound.
4. Infeasible user entries produce a clear error without altering the board.
5. The frontend source is inside the Spring Boot project (`src/main/frontend`) and a single `mvn package` produces one jar that serves both the UI and the API.
6. All tests pass.

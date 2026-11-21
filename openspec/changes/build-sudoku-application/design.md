# Design

## Context

The project has no application code or existing capability specs. The source of product requirements is `specification/specification.md`; see the proposal for motivation. The implementation must target Java 25 and deliver the Vue UI from the Spring Boot application.

## Goals / Non-Goals

**Goals:**
- Separate board validation, integral LP solving, generation, HTTP handling, and client state so each is independently testable.
- Preserve proven uniqueness and distinguish incomplete search from infeasibility.
- Bound resource use and keep simultaneous requests isolated.

**Non-Goals:**
- User accounts, server-side game persistence, multiplayer, or difficulty grading.
- A separate production frontend deployment or an alternate non-LP automatic solver.
- Guaranteeing generation or solve success for every board before the configured limits.

## Decisions

### Single Maven project and same-origin application

Keep Vue 3, TypeScript, and Vite under `src/main/frontend`. Use `frontend-maven-plugin` to install a pinned Node/npm toolchain and execute frontend installation, tests, and build during Maven's resource-generation lifecycle; Vite emits to `src/main/resources/static`, copied into the jar by Maven. Maven targets release 25 and uses a Spring Boot 3.x release documented as compatible with Java 25. `spring-boot:run` must also trigger frontend generation; `-DskipTests` skips frontend and backend tests, not the build. Vite development mode proxies `/api` to port 8080, while production uses relative paths and does not require CORS. Prefer a single-view UI; add an SPA fallback only if routing actually requires one.

This follows the requested integrated deployment and avoids coordinating two production services. A separate frontend server was rejected because it violates that requirement.

### Board representation and validation

Use N-by-N integer boards with 0 for empty, calculate boxRows as the largest divisor of N at most its square root, and boxCols as N / boxRows. The server validates shape, integer range, dimensions, and clue preservation before solving. Detect immediate duplicate entries as infeasible (422), not malformed input (400). Independently validate all returned solutions and retain no shared per-game state. Prime N gives 1-by-N regions and is communicated as a Latin square.

### LP solver and uniqueness

For each cell (i,j) and value k, model binary x[i,j,k], indexed zero-based by `(i*N+j)*N+k`. Minimize the zero objective subject to one value per cell, one occurrence of each value in each row, column, and region, fixed nonzero entries, and bounds 0 <= x <= 1. Apache Commons Math 3.6.1 `SimplexSolver` handles the continuous relaxation, not integrality; depth-first branch-and-bound branches x=1 and x=0 on a fractional variable until an integral result is found. Treat values within 1e-6 of 0 or 1 as integral and independently verify the decoded board. Numerical errors or invalid decoded boards are explicit internal errors, never solutions or proof of infeasibility.

For uniqueness, find a first validated solution and add the no-good constraint `sum(x[i,j,solution[i,j]]) <= N*N-1`; a proven infeasible second search establishes uniqueness, while a second solution establishes non-uniqueness. An exhausted budget is inconclusive, never proof of uniqueness. Each search carries a deadline, node limit and simplex iteration limit; defaults are 30 seconds and 10,000 nodes, with a configurable iteration limit. Map exhaustion to 504, not 422. Direct use of continuous simplex alone was rejected because fractional feasible points are not Sudoku solutions.

### Generation and shared budget

Create a valid complete board from a rectangular-region-compatible pattern and randomized value, within-band row, within-stack column, band, and stack permutations. Remove clues in random order; accept a removal only after the LP-based second-solution check proves uniqueness. Keep the last proven puzzle separately from the candidate. A single overall generation deadline, default 10 seconds, includes all uniqueness checks, which also respect solver node and iteration caps. At the deadline or an inconclusive check, discard the candidate; return the last proven puzzle if it has an empty cell, otherwise return 504. Do not send the completed board to the client. A simple unconstrained removal algorithm was rejected because it cannot ensure uniqueness.

### Frontend state and API

Keep puzzle clues, editable board, selection, conflicts and game status in a Vue composable. The UI provides keyboard and number-pad entry, including multi-digit values; clues are immutable, reset clears entries, and client-side unit validation detects conflicts and completion. Solve sends both boards and disables conflicting controls; failures retain the board. Errors use `{code, message}` and success follows the generate and solve contracts. An optional validate endpoint, timer, and move counter are not required.

## Risks / Trade-offs

- [LP cost grows sharply with N and repeated uniqueness checks] → Apply sound constraint preprocessing, enforce budgets, and return a clear timeout rather than claiming success. Validate representative 4, 6, and 9 puzzles; measure 9-by-9 typical solve under 2 seconds and 16-by-16 bounded at 30 seconds, documenting hardware and timeouts.
- [Simplex may spend time in one optimization call] → Enforce deadline during the call, not only between nodes, using a cancellation mechanism verified to stop computation; if cooperative cancellation is unavailable, isolate solver work in a terminable worker. Test timeout and cleanup. An iteration cap alone is not a wall-clock deadline.
- [Floating-point feasibility differs from integer validity] → Validate every decoded board against all constraints; distinguish numerical failures from infeasibility.
- [Frontend assets written to source resources may leave stale output] → Clean Vite output before building and verify a clean Maven package contains fresh assets. Never remove manually maintained static resources.
- [Generated puzzle may contain few clues but no difficulty classification] → Promise only uniqueness, not a particular difficulty or minimality.

## Migration Plan

This is a new application with no existing deployment or persisted data. Build and smoke-test the single jar, deploy it as a new service, and roll back by removing or replacing that jar.

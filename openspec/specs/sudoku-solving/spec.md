# sudoku-solving Specification

## Purpose

Provide validated automatic Sudoku solutions while preserving clues and current player entries.

## Requirements

### Requirement: Solve API input
The `POST /api/sudoku/solve` endpoint SHALL accept JSON fields `size`, `boxRows`, `boxCols`, `puzzle`, and `current`. Both boards SHALL be N-by-N arrays of integers from 0 through N; 0 means empty. Region dimensions SHALL match the generation rules and current SHALL retain every nonzero puzzle clue.

#### Scenario: Malformed request
- **WHEN** a request contains an invalid size, dimensions, shape, value, or a changed or cleared clue
- **THEN** the API returns HTTP 400 with a structured error.

### Requirement: Integral automatic solution
The system SHALL solve the submitted puzzle and all nonzero current entries as fixed constraints, using linear programming with Apache Commons Math and an integrality method. It SHALL independently validate the decoded board before returning it.

#### Scenario: Valid solution
- **WHEN** a valid solvable request completes within the solver limits
- **THEN** HTTP 200 returns an N-by-N `solution` preserving the clues and current entries, with every value 1 through N appearing exactly once in each row, column, and region.

### Requirement: Explicit infeasibility and resource limits
The solve endpoint SHALL distinguish proven infeasibility from a solver resource limit and SHALL NOT return partial or unvalidated solutions as successes.

#### Scenario: Conflicting or impossible current entries
- **WHEN** the current entries have no completion consistent with the puzzle
- **THEN** the API returns HTTP 422 with a structured infeasibility error.

#### Scenario: Solver limit reached
- **WHEN** the configured time, node, or simplex iteration limit is reached before a solution or infeasibility is established
- **THEN** the API returns HTTP 504 with a structured limit error.

### Requirement: Solve response metadata and error format
Successful solve responses SHALL include `solution`, `solveTimeMs`, and `nodesExplored`. API error responses SHALL include string `code` and `message` fields.

#### Scenario: Successful solve metadata
- **WHEN** a solve request succeeds
- **THEN** its response includes the solution, nonnegative elapsed milliseconds, and the number of explored solver nodes.

#### Scenario: Solve error body
- **WHEN** a solve request fails validation, is infeasible, or exceeds a limit
- **THEN** its response includes a machine-readable `code` and a human-readable `message`.

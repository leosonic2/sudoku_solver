# sudoku-generation Specification

## Purpose

Enable users to request uniquely solvable Sudoku puzzles with predictable board and region dimensions.

## Requirements

### Requirement: Supported board sizes and regions
The system SHALL accept integer sizes N from 2 through 16, inclusive, defaulting to 9 in the UI. A board SHALL contain N rows, N columns, values 1 through N, and rectangular regions with boxRows equal to the largest divisor of N not exceeding its square root and boxCols equal to N / boxRows.

#### Scenario: Rectangular regions
- **WHEN** a puzzle of size 6 is requested
- **THEN** the board has 6 rows and columns and 2-by-3 regions.

#### Scenario: Prime-sized regions
- **WHEN** a puzzle of prime size is generated
- **THEN** regions have one row and N columns and the UI explains that the resulting puzzle is a Latin square.

#### Scenario: Unsupported size
- **WHEN** the generation API receives a non-integer or a size outside 2 through 16
- **THEN** it returns HTTP 400 with a structured error and no puzzle.

### Requirement: Generate a uniquely solvable puzzle
The system SHALL generate a puzzle with at least one empty cell, exactly one valid solution, and no contradictory clues. Empty cells SHALL be represented by 0.

#### Scenario: Generation succeeds
- **WHEN** generation for a supported size finishes successfully
- **THEN** the puzzle has an N-by-N shape, at least one zero, and exactly one valid solution preserving all clues.

#### Scenario: Generation limit is reached before a usable puzzle
- **WHEN** the generation budget is exhausted before a puzzle with at least one empty cell and proven uniqueness is available
- **THEN** the API returns HTTP 504 with a structured error rather than an unverified puzzle.

#### Scenario: Generation limit is reached after a usable puzzle
- **WHEN** the generation budget is exhausted after a puzzle with at least one empty cell and proven uniqueness is available
- **THEN** the system can return that last verified puzzle without claiming an incomplete uniqueness check succeeded.

### Requirement: Generation API contract
The `POST /api/sudoku/generate` endpoint SHALL accept a JSON `size` and return `size`, `boxRows`, `boxCols`, and `puzzle` on success without exposing the solution.

#### Scenario: Successful response
- **WHEN** a valid size 9 generation request succeeds
- **THEN** HTTP 200 returns size 9, boxRows 3, boxCols 3 and a 9-by-9 puzzle, without a solution field.

# sudoku-gameplay Specification

## Purpose

Provide an accessible interactive Sudoku game with clear editing, feedback, and solving controls.

## Requirements

### Requirement: Render and edit the board
The UI SHALL display an N-by-N grid with visible region boundaries and visually distinct, immutable clues. Editable cells SHALL accept values 1 through N and allow clearing.

#### Scenario: Input and clear a multi-digit value
- **WHEN** a player enters 12 in an editable cell of a size-12 puzzle and then clears it
- **THEN** the cell first shows 12 and then becomes empty, without changing any clue.

#### Scenario: Attempt to edit a clue
- **WHEN** a player tries to change an original clue
- **THEN** the clue remains unchanged.

### Requirement: Accessible board interaction
The UI SHALL support arrow-key cell navigation, keyboard entry including multi-digit values, an on-screen number pad, and accessible cell labels. It SHALL highlight the selected cell and its row, column, and region and adapt to mobile and desktop screens.

#### Scenario: Navigate by keyboard
- **WHEN** a player selects a cell and presses an arrow key
- **THEN** focus moves to the adjacent available cell and the corresponding selection highlights update.

### Requirement: Conflict and completion feedback
The UI SHALL highlight duplicate nonzero values in a row, column, or region and announce completion only when every cell is filled, all units contain values 1 through N exactly once, and clues are preserved.

#### Scenario: Conflicting entry
- **WHEN** a player enters a value already present in the same row, column, or region
- **THEN** the conflicting cells are highlighted and the puzzle is not reported solved.

#### Scenario: Valid completed board
- **WHEN** the player completes a valid board
- **THEN** the UI reports that the puzzle is solved.

### Requirement: Reset and replace a game
The UI SHALL provide a reset control that clears player entries but preserves clues and SHALL request confirmation before replacing a game in progress.

#### Scenario: Reset current game
- **WHEN** the player resets a game with user-entered values
- **THEN** all user entries are cleared and all original clues remain unchanged.

#### Scenario: Cancel replacement
- **WHEN** the player declines confirmation to generate a new puzzle during a game in progress
- **THEN** the current board remains unchanged.

### Requirement: Automatic solve interaction
The UI SHALL submit the original clues and current entries to the solve endpoint, display a loading state, and disable Generate, Solve, and Reset while the request is pending.

#### Scenario: Apply solution
- **WHEN** the solve request succeeds
- **THEN** the UI fills the returned solution, distinguishes solver-filled cells, marks the game solved automatically, and displays the solve time.

#### Scenario: Unsolvable entries or solver failure
- **WHEN** the solve request fails, including infeasible player entries or a resource limit
- **THEN** the UI displays a clear error, preserves the board, and re-enables the controls.

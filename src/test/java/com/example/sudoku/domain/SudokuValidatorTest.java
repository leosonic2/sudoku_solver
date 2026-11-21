package com.example.sudoku.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SudokuValidatorTest {
    @Test
    void calculatesSquareRectangularAndPrimeRegions() {
        assertEquals(new BoxDimensions(3, 3), BoxDimensions.forSize(9));
        assertEquals(new BoxDimensions(2, 3), BoxDimensions.forSize(6));
        assertEquals(new BoxDimensions(1, 5), BoxDimensions.forSize(5));
        assertThrows(IllegalArgumentException.class, () -> BoxDimensions.validate(6, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> BoxDimensions.forSize(17));
    }

    @Test
    void rejectsMalformedBoardsAndPreservesItsOwnCopy() {
        assertThrows(IllegalArgumentException.class, () -> new Board(4, null));
        assertThrows(IllegalArgumentException.class, () -> new Board(4, new int[3][4]));
        assertThrows(IllegalArgumentException.class, () -> new Board(4, new int[][] {
                {1, 2, 3, 4}, {1, 2, 3, 4}, null, {1, 2, 3, 4}
        }));
        assertThrows(IllegalArgumentException.class, () -> new Board(4, new int[][] {
                {5, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}
        }));
        int[][] cells = {{1, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}};
        Board board = new Board(4, cells);
        cells[0][0] = 2;
        int[][] copy = board.copyCells();
        copy[0][0] = 3;
        assertEquals(1, board.get(0, 0));
        assertArrayEquals(new int[] {1, 0, 0, 0}, board.copyCells()[0]);
    }

    @Test
    void rejectsChangedOrClearedClues() {
        Board puzzle = new Board(4, new int[][] {
                {1, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}
        });
        Board changed = new Board(4, new int[][] {
                {2, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}
        });
        Board cleared = new Board(4, new int[4][4]);
        assertThrows(IllegalArgumentException.class, () -> SudokuValidator.validateClues(puzzle, changed));
        assertThrows(IllegalArgumentException.class, () -> SudokuValidator.validateClues(puzzle, cleared));
        SudokuValidator.validateClues(puzzle, puzzle);
    }

    @Test
    void detectsRowColumnAndRegionContradictions() {
        int[][] cells = new int[6][6];
        cells[0][0] = 1;
        cells[0][4] = 1;
        assertTrue(SudokuValidator.hasConflicts(new Board(6, cells)));
        cells[0][4] = 0;
        cells[3][0] = 1;
        assertTrue(SudokuValidator.hasConflicts(new Board(6, cells)));
        cells[3][0] = 0;
        cells[1][2] = 1;
        assertTrue(SudokuValidator.hasConflicts(new Board(6, cells)));
        cells[1][2] = 0;
        assertFalse(SudokuValidator.hasConflicts(new Board(6, cells)));
        assertFalse(SudokuValidator.isCompleteAndValid(new Board(6, cells)));
    }

    @Test
    void acceptsValidCompleteRectangularAndPrimeBoards() {
        Board square = new Board(4, new int[][] {
                {1, 2, 3, 4}, {3, 4, 1, 2}, {2, 1, 4, 3}, {4, 3, 2, 1}
        });
        Board rectangular = new Board(6, new int[][] {
                {1, 2, 3, 4, 5, 6}, {4, 5, 6, 1, 2, 3},
                {2, 3, 4, 5, 6, 1}, {5, 6, 1, 2, 3, 4},
                {3, 4, 5, 6, 1, 2}, {6, 1, 2, 3, 4, 5}
        });
        Board prime = new Board(3, new int[][] {{1, 2, 3}, {2, 3, 1}, {3, 1, 2}});
        Board contradictory = new Board(3, new int[][] {{1, 1, 3}, {2, 3, 1}, {3, 1, 2}});
        assertTrue(SudokuValidator.isCompleteAndValid(square));
        assertTrue(SudokuValidator.isCompleteAndValid(rectangular));
        assertTrue(SudokuValidator.isCompleteAndValid(prime));
        assertFalse(SudokuValidator.isCompleteAndValid(contradictory));
    }
}

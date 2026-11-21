import { expect, test } from 'vitest'
import { useSudokuGame } from './useSudokuGame'

const solution = [[1, 2, 3, 4], [3, 4, 1, 2], [2, 1, 4, 3], [4, 3, 2, 1]]
const puzzle = [[1, 0, 0, 4], [0, 4, 1, 0], [0, 1, 4, 0], [4, 0, 0, 1]]

test('preserves clues, resets entries, and recognizes a valid completed board', () => {
  const game = useSudokuGame()
  game.start({ size: 4, boxRows: 2, boxCols: 2, puzzle })
  game.setValue(2)
  expect(game.board.value[0][0]).toBe(1)
  for (let row = 0; row < 4; row++) {
    for (let column = 0; column < 4; column++) {
      game.selected.value = { row, column }
      game.setValue(solution[row][column])
    }
  }
  expect(game.complete.value).toBe(true)
  game.reset()
  expect(game.complete.value).toBe(false)
  expect(game.board.value).toEqual(puzzle)
})

test('detects and clears row, column, and region conflicts', () => {
  const game = useSudokuGame()
  game.start({ size: 4, boxRows: 2, boxCols: 2, puzzle: Array.from({ length: 4 }, () => [0, 0, 0, 0]) })
  game.setValue(1)
  game.selected.value = { row: 1, column: 1 }
  game.setValue(1)
  expect(game.conflicts.value.has('0:0')).toBe(true)
  expect(game.conflicts.value.has('1:1')).toBe(true)
  game.selected.value = { row: 0, column: 2 }
  game.setValue(1)
  game.selected.value = { row: 2, column: 0 }
  game.setValue(1)
  expect(game.conflicts.value.has('0:2')).toBe(true)
  expect(game.conflicts.value.has('2:0')).toBe(true)
  game.reset()
  expect(game.conflicts.value.size).toBe(0)
  expect(game.complete.value).toBe(false)
})

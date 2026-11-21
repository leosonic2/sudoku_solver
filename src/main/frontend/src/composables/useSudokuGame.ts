import { computed, ref } from 'vue'
import type { PuzzleResponse } from '../types/sudoku'

export interface CellPosition {
  row: number
  column: number
}

export function useSudokuGame() {
  const game = ref<PuzzleResponse | null>(null)
  const board = ref<number[][]>([])
  const selected = ref<CellPosition>({ row: 0, column: 0 })
  const solverFilled = ref(new Set<string>())
  const automaticallySolved = ref(false)
  const solveTimeMs = ref<number | null>(null)

  function start(puzzle: PuzzleResponse) {
    game.value = { ...puzzle, puzzle: puzzle.puzzle.map(row => [...row]) }
    board.value = puzzle.puzzle.map(row => [...row])
    selected.value = { row: 0, column: 0 }
    solverFilled.value = new Set()
    automaticallySolved.value = false
    solveTimeMs.value = null
  }

  function setValue(value: number) {
    const current = game.value
    const { row, column } = selected.value
    if (!current || current.puzzle[row][column] !== 0 || !Number.isInteger(value)
        || value < 0 || value > current.size) return
    if (board.value[row][column] !== value) {
      board.value[row][column] = value
      solverFilled.value.delete(`${row}:${column}`)
      automaticallySolved.value = false
    }
  }

  function reset() {
    if (game.value) board.value = game.value.puzzle.map(row => [...row])
    solverFilled.value = new Set()
    automaticallySolved.value = false
    solveTimeMs.value = null
  }

  function applySolution(solution: number[][], milliseconds: number) {
    const filled = new Set<string>()
    board.value.forEach((row, r) => row.forEach((value, c) => {
      if (value === 0) filled.add(`${r}:${c}`)
    }))
    board.value = solution.map(row => [...row])
    solverFilled.value = filled
    automaticallySolved.value = true
    solveTimeMs.value = milliseconds
  }

  const conflicts = computed(() => {
    const result = new Set<string>()
    const current = game.value
    if (!current) return result
    const units: CellPosition[][] = []
    for (let i = 0; i < current.size; i++) {
      units.push(Array.from({ length: current.size }, (_, column) => ({ row: i, column })))
      units.push(Array.from({ length: current.size }, (_, row) => ({ row, column: i })))
    }
    for (let row = 0; row < current.size; row += current.boxRows) {
      for (let column = 0; column < current.size; column += current.boxCols) {
        const cells: CellPosition[] = []
        for (let r = row; r < row + current.boxRows; r++) {
          for (let c = column; c < column + current.boxCols; c++) cells.push({ row: r, column: c })
        }
        units.push(cells)
      }
    }
    for (const unit of units) {
      const positions = new Map<number, CellPosition[]>()
      for (const cell of unit) {
        const value = board.value[cell.row][cell.column]
        if (value) positions.set(value, [...(positions.get(value) ?? []), cell])
      }
      for (const matches of positions.values()) {
        if (matches.length > 1) matches.forEach(cell => result.add(`${cell.row}:${cell.column}`))
      }
    }
    return result
  })

  const complete = computed(() => {
    const current = game.value
    return !!current && conflicts.value.size === 0 && board.value.every((row, r) =>
      row.every((value, c) => value >= 1 && value <= current.size
        && (current.puzzle[r][c] === 0 || current.puzzle[r][c] === value)),
    )
  })

  return { game, board, selected, conflicts, complete, solverFilled, automaticallySolved,
    solveTimeMs, start, setValue, reset, applySolution }
}

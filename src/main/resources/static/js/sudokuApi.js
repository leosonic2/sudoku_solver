function isRecord(value) {
  return typeof value === 'object' && value !== null
}

function isBoard(value, size) {
  return Array.isArray(value) && value.length === size && value.every(row =>
    Array.isArray(row) && row.length === size
      && row.every(cell => Number.isInteger(cell) && cell >= 0 && cell <= size),
  )
}

function validSolution(solution, game, current) {
  const { size, boxRows, boxCols } = game
  for (let row = 0; row < size; row++) {
    const values = new Set()
    for (let column = 0; column < size; column++) {
      const value = solution[row][column]
      if (value === 0 || (current[row][column] !== 0 && current[row][column] !== value)) return false
      values.add(value)
    }
    if (values.size !== size) return false
  }
  for (let column = 0; column < size; column++) {
    if (new Set(solution.map(row => row[column])).size !== size) return false
  }
  for (let row = 0; row < size; row += boxRows) {
    for (let column = 0; column < size; column += boxCols) {
      const values = new Set()
      for (let r = row; r < row + boxRows; r++) {
        for (let c = column; c < column + boxCols; c++) values.add(solution[r][c])
      }
      if (values.size !== size) return false
    }
  }
  return true
}

async function post(path, request) {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const body = await response.json()
  if (!response.ok) {
    const message = isRecord(body) && typeof body.message === 'string'
      ? body.message : `Request failed with HTTP ${response.status}.`
    throw new Error(message)
  }
  return body
}

export async function generatePuzzle(size) {
  const result = await post('/api/sudoku/generate', { size })
  let boxRows = Math.floor(Math.sqrt(size))
  while (size % boxRows !== 0) boxRows--
  if (!isRecord(result) || result.size !== size || typeof result.boxRows !== 'number'
      || result.boxRows !== boxRows || typeof result.boxCols !== 'number'
      || result.boxCols !== size / boxRows || !isBoard(result.puzzle, size)) {
    throw new Error('The server returned an invalid puzzle.')
  }
  return { size, boxRows: result.boxRows, boxCols: result.boxCols, puzzle: result.puzzle }
}

export async function solvePuzzle(game, current) {
  const result = await post('/api/sudoku/solve', { ...game, current })
  if (!isRecord(result) || !isBoard(result.solution, game.size)
      || !validSolution(result.solution, game, current)
      || typeof result.solveTimeMs !== 'number' || result.solveTimeMs < 0
      || typeof result.nodesExplored !== 'number' || result.nodesExplored < 0) {
    throw new Error('The server returned an invalid solution.')
  }
  return { solution: result.solution, solveTimeMs: result.solveTimeMs, nodesExplored: result.nodesExplored }
}

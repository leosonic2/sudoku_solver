export interface PuzzleResponse {
  size: number
  boxRows: number
  boxCols: number
  puzzle: number[][]
}

export interface SolveResponse {
  solution: number[][]
  solveTimeMs: number
  nodesExplored: number
}

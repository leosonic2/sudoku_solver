import { createApp, ref } from './vue.js'
import { generatePuzzle, solvePuzzle } from './sudokuApi.js'
import SizeSelector from './SizeSelector.js'
import SudokuBoard from './SudokuBoard.js'
import { useSudokuGame } from './useSudokuGame.js'

createApp({
  components: { SizeSelector, SudokuBoard },
  setup() {
    const gameState = useSudokuGame()
    const { game, board, complete, start, applySolution } = gameState
    const busy = ref(false)
    const busyMessage = ref('')
    const error = ref('')

    async function generate(size) {
      if (busy.value || (game.value && !complete.value && !window.confirm('Replace the current game?'))) return
      busy.value = true
      busyMessage.value = 'Generating puzzle...'
      error.value = ''
      try {
        start(await generatePuzzle(size))
      } catch (cause) {
        error.value = cause instanceof Error ? cause.message : 'Unable to generate a puzzle.'
      } finally {
        busy.value = false
      }
    }

    async function solve() {
      if (!game.value || busy.value) return
      busy.value = true
      busyMessage.value = 'Solving puzzle...'
      error.value = ''
      try {
        const result = await solvePuzzle(game.value, board.value.map(row => [...row]))
        applySolution(result.solution, result.solveTimeMs)
      } catch (cause) {
        error.value = cause instanceof Error ? cause.message : 'Unable to solve the puzzle.'
      } finally {
        busy.value = false
      }
    }

    return { ...gameState, busy, busyMessage, error, generate, solve }
  },
  template: `
    <main>
      <h1>Sudoku</h1>
      <SizeSelector :busy="busy" @generate="generate" />
      <p v-if="busy" role="status">{{ busyMessage }}</p>
      <p v-if="error" role="alert">{{ error }}</p>
      <section v-if="game" aria-label="Game">
        <p>{{ game.size }}-by-{{ game.size }} puzzle ready.</p>
        <SudokuBoard :game="game" :board="board" :selected="selected"
                     :conflicts="conflicts" :solver-filled="solverFilled" :busy="busy"
                     @select="selected = $event" @input="setValue" />
        <div class="number-pad" aria-label="Number pad">
          <button v-for="number in game.size" :key="number" type="button" :disabled="busy"
                  @click="setValue(number)">{{ number }}</button>
          <button type="button" :disabled="busy" @click="setValue(0)">Clear</button>
        </div>
        <button type="button" :disabled="busy" @click="reset">Reset</button>
        <button type="button" :disabled="busy" @click="solve">Solve Automatically</button>
        <p v-if="conflicts.size" role="status">Conflicting entries are highlighted.</p>
        <p v-else-if="automaticallySolved" role="status">Puzzle solved automatically in {{ solveTimeMs }} ms.</p>
        <p v-else-if="complete" role="status">Puzzle solved!</p>
      </section>
    </main>
  `,
}).mount('#app')

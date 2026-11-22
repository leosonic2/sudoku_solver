import { nextTick, ref } from './vue.js'

export default {
  props: ['game', 'board', 'selected', 'conflicts', 'solverFilled', 'busy'],
  emits: ['select', 'input'],
  setup(props, { emit }) {
    const grid = ref(null)
    let lastCell = ''
    let lastDigit = ''
    let lastTime = 0

    function related(row, column) {
      return row === props.selected.row || column === props.selected.column
        || (Math.floor(row / props.game.boxRows) === Math.floor(props.selected.row / props.game.boxRows)
          && Math.floor(column / props.game.boxCols) === Math.floor(props.selected.column / props.game.boxCols))
    }

    async function keydown(event, row, column) {
      const directions = {
        ArrowUp: [-1, 0], ArrowDown: [1, 0], ArrowLeft: [0, -1], ArrowRight: [0, 1],
      }
      if (event.key in directions) {
        event.preventDefault()
        const [dr, dc] = directions[event.key]
        const next = {
          row: Math.max(0, Math.min(props.game.size - 1, row + dr)),
          column: Math.max(0, Math.min(props.game.size - 1, column + dc)),
        }
        lastDigit = ''
        emit('select', next)
        await nextTick()
        grid.value?.querySelector(`[data-cell="${next.row}:${next.column}"]`)?.focus()
        return
      }
      if (event.key === 'Backspace' || event.key === 'Delete') {
        event.preventDefault()
        lastDigit = ''
        emit('input', 0)
        return
      }
      if (!/^[0-9]$/.test(event.key)) return
      event.preventDefault()
      const cell = `${row}:${column}`
      const now = Date.now()
      const combined = cell === lastCell && now - lastTime < 1000 ? Number(lastDigit + event.key) : -1
      const digit = Number(event.key)
      if (lastDigit && combined >= 1 && combined <= props.game.size) {
        emit('input', combined)
        lastDigit = ''
      } else {
        emit('input', digit)
        lastDigit = digit > 0 && digit * 10 <= props.game.size ? event.key : ''
      }
      lastCell = cell
      lastTime = now
    }

    return { grid, related, keydown, emit }
  },
  template: `
    <div ref="grid" class="grid" role="grid" aria-label="Sudoku board"
         :style="{ '--size': game.size }">
      <div v-for="(row, r) in board" :key="r" role="row" class="grid-row">
        <button v-for="(value, c) in row" :key="c" type="button" role="gridcell"
                :data-cell="r + ':' + c"
                :tabindex="selected.row === r && selected.column === c ? 0 : -1"
                :aria-label="'Row ' + (r + 1) + ', column ' + (c + 1) + ', ' + (game.puzzle[r][c] ? 'clue' : 'entry') + ' ' + (value || 'empty')"
                :aria-selected="selected.row === r && selected.column === c"
                :aria-readonly="game.puzzle[r][c] !== 0"
                :aria-invalid="conflicts.has(r + ':' + c)"
                :disabled="busy"
                :class="{
                  clue: game.puzzle[r][c] !== 0,
                  related: related(r, c),
                  selected: selected.row === r && selected.column === c,
                  conflict: conflicts.has(r + ':' + c),
                  'solver-filled': solverFilled.has(r + ':' + c),
                  'box-top': r % game.boxRows === 0,
                  'box-left': c % game.boxCols === 0,
                }"
                @focus="emit('select', { row: r, column: c })"
                @click="emit('select', { row: r, column: c })"
                @keydown="keydown($event, r, c)">{{ value || '' }}</button>
      </div>
    </div>
  `,
}

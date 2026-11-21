import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, expect, test, vi } from 'vitest'
import App from './App.vue'

const puzzle = {
  size: 4, boxRows: 2, boxCols: 2,
  puzzle: [[1, 0, 0, 4], [0, 4, 1, 0], [0, 1, 4, 0], [4, 0, 0, 1]],
}

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

test('validates sizes and explains prime-size regions', async () => {
  const fetch = vi.fn()
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App)
  await wrapper.get('input').setValue('17')
  await wrapper.get('form').trigger('submit')
  expect(wrapper.text()).toContain('Choose an integer from 2 to 16.')
  expect(fetch).not.toHaveBeenCalled()
  await wrapper.get('input').setValue('5')
  expect(wrapper.text()).toContain('Latin square')
  await wrapper.get('input').setValue('6')
  expect(wrapper.text()).not.toContain('Latin square')
})

test('generates a puzzle and can cancel replacing it', async () => {
  const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => puzzle })
  vi.stubGlobal('fetch', fetch)
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false)
  const wrapper = mount(App)
  await wrapper.get('input').setValue('4')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
  expect(fetch).toHaveBeenCalledWith('/api/sudoku/generate', expect.objectContaining({
    body: JSON.stringify({ size: 4 }),
  }))
  expect(wrapper.text()).toContain('4-by-4 puzzle ready.')
  await wrapper.get('form').trigger('submit')
  expect(confirm).toHaveBeenCalledOnce()
  expect(fetch).toHaveBeenCalledOnce()
  expect(wrapper.text()).toContain('4-by-4 puzzle ready.')
})

test('reports generation errors and keeps existing controls usable', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: false, status: 504, json: async () => ({ code: 'LIMIT_REACHED', message: 'Generation timed out' }),
  }))
  const wrapper = mount(App)
  await wrapper.get('form').trigger('submit')
  await flushPromises()
  expect(wrapper.get('[role="alert"]').text()).toContain('Generation timed out')
  expect(wrapper.get('button').attributes('disabled')).toBeUndefined()
})

test('supports grid navigation, number pad, conflicts, clues, and reset', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => puzzle }))
  const wrapper = mount(App, { attachTo: document.body })
  await wrapper.get('input').setValue('4')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
  const clue = wrapper.get('[data-cell="0:0"]')
  ;(clue.element as HTMLButtonElement).focus()
  await clue.trigger('keydown', { key: '2' })
  expect(clue.text()).toBe('1')
  await clue.trigger('keydown', { key: 'ArrowRight' })
  expect(document.activeElement?.getAttribute('data-cell')).toBe('0:1')
  const entry = wrapper.get('[data-cell="0:1"]')
  expect(entry.attributes('aria-label')).toContain('Row 1, column 2')
  expect(wrapper.get('[data-cell="1:0"]').classes()).toContain('related')
  expect(wrapper.get('[data-cell="3:3"]').classes()).not.toContain('related')
  expect(wrapper.get('[data-cell="0:2"]').classes()).toContain('box-left')
  expect(wrapper.get('[data-cell="2:0"]').classes()).toContain('box-top')
  const four = wrapper.findAll('.number-pad button').find(button => button.text() === '4')
  expect(four).toBeDefined()
  await four!.trigger('click')
  expect(entry.text()).toBe('4')
  expect(entry.attributes('aria-invalid')).toBe('true')
  expect(wrapper.text()).toContain('Conflicting entries')
  await wrapper.findAll('button').find(button => button.text() === 'Reset')!.trigger('click')
  expect(entry.text()).toBe('')
  expect(clue.text()).toBe('1')
  wrapper.unmount()
})

test('enters and clears multi-digit values by keyboard', async () => {
  const large = {
    size: 12, boxRows: 3, boxCols: 4,
    puzzle: Array.from({ length: 12 }, () => Array(12).fill(0)),
  }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => large }))
  const wrapper = mount(App, { attachTo: document.body })
  await wrapper.get('input').setValue('12')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
  const cell = wrapper.get('[data-cell="0:0"]')
  ;(cell.element as HTMLButtonElement).focus()
  await cell.trigger('keydown', { key: '1' })
  await cell.trigger('keydown', { key: '2' })
  expect(cell.text()).toBe('12')
  await cell.trigger('keydown', { key: 'Backspace' })
  expect(cell.text()).toBe('')
  wrapper.unmount()
})

test('solves using current entries, disables controls while pending, and styles solver-filled cells', async () => {
  const solution = [[1, 2, 3, 4], [3, 4, 1, 2], [2, 1, 4, 3], [4, 3, 2, 1]]
  let resolveResponse: (value: { ok: boolean, json: () => Promise<unknown> }) => void = () => {}
  const pending = new Promise<{ ok: boolean, json: () => Promise<unknown> }>(resolve => { resolveResponse = resolve })
  const fetch = vi.fn()
    .mockResolvedValueOnce({ ok: true, json: async () => puzzle })
    .mockReturnValueOnce(pending)
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App)
  await wrapper.get('input').setValue('4')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
  await wrapper.get('[data-cell="0:1"]').trigger('focus')
  await wrapper.findAll('.number-pad button').find(button => button.text() === '2')!.trigger('click')
  const solve = wrapper.findAll('button').find(button => button.text() === 'Solve Automatically')!
  await solve.trigger('click')
  expect(wrapper.text()).toContain('Solving puzzle...')
  for (const label of ['Generate', 'Reset', 'Solve Automatically']) {
    expect(wrapper.findAll('button').find(button => button.text() === label)!.attributes('disabled')).toBeDefined()
  }
  const request = JSON.parse(fetch.mock.calls[1][1].body)
  expect(request.current[0][1]).toBe(2)
  resolveResponse({ ok: true, json: async () => ({ solution, solveTimeMs: 42, nodesExplored: 1 }) })
  await flushPromises()
  expect(wrapper.text()).toContain('Puzzle solved automatically in 42 ms.')
  expect(wrapper.get('[data-cell="0:2"]').text()).toBe('3')
  expect(wrapper.get('[data-cell="0:2"]').classes()).toContain('solver-filled')
  expect(wrapper.get('[data-cell="0:1"]').classes()).not.toContain('solver-filled')
  expect(wrapper.get('[data-cell="0:0"]').classes()).not.toContain('solver-filled')
  expect(solve.attributes('disabled')).toBeUndefined()
})

test.each([
  [422, 'Your current entries make the puzzle unsolvable'],
  [504, 'Solver time limit reached'],
])('preserves entries after solve error %i', async (status, message) => {
  vi.stubGlobal('fetch', vi.fn()
    .mockResolvedValueOnce({ ok: true, json: async () => puzzle })
    .mockResolvedValueOnce({ ok: false, status, json: async () => ({ message }) }))
  const wrapper = mount(App)
  await wrapper.get('input').setValue('4')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
  await wrapper.get('[data-cell="0:1"]').trigger('focus')
  await wrapper.findAll('.number-pad button').find(button => button.text() === '2')!.trigger('click')
  await wrapper.findAll('button').find(button => button.text() === 'Solve Automatically')!.trigger('click')
  await flushPromises()
  expect(wrapper.get('[role="alert"]').text()).toContain(message)
  expect(wrapper.get('[data-cell="0:1"]').text()).toBe('2')
  expect(wrapper.findAll('button').find(button => button.text() === 'Reset')!.attributes('disabled')).toBeUndefined()
})

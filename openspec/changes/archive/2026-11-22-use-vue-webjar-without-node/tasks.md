# Tasks

## 1. WebJar and Node-free test foundation

- [x] 1.1 Add the pinned Vue 3 WebJar and a Spring resource test that verifies the versioned full browser build is served with a JavaScript content type.
- [x] 1.2 Add test-scoped Selenium/JUnit browser setup and deterministic same-origin API fixtures; verify a headless browser can load a Spring-served test page and missing browser or driver prerequisites fail clearly.
- [x] 1.3 Document the browser and driver prerequisites and first-run downloads in `README.md`; verify the documented setup runs without Node.js or npm.
- [x] 1.4 Make `src/main/resources/static` trackable in `.gitignore` and establish browser-ready HTML, CSS, and JavaScript entry points; verify Git sees the source files and the browser loads Vue from the WebJar.

## 2. Migrate the interface with regression coverage

- [x] 2.1 Translate the Sudoku game state and API client to browser JavaScript, preserving response validation and same-origin requests; verify generation, valid solve, malformed responses, and server errors with controlled browser tests.
- [x] 2.2 Translate the size selector, game controls, confirmation, loading states, and styles; verify invalid sizes, prime-size guidance, replacement cancellation, and disabled controls during requests in browser tests.
- [x] 2.3 Translate the board, keyboard navigation, multi-digit entry, number pad, clues, responsive styling, and accessibility attributes; verify input, clearing, focus movement, labels, and clue immutability in browser tests.
- [x] 2.4 Preserve conflicts, completion, reset, and automatic solve feedback including solver-filled cells and timing; verify success and failure scenarios without losing user entries in browser tests.

## 3. Remove the old frontend toolchain

- [x] 3.1 Remove `frontend-maven-plugin` and the obsolete npm, TypeScript, Vite, Vue single-file component, and Vitest sources after their replacement tests pass; verify Maven does not install or invoke Node.js or npm.
- [x] 3.2 Remove stale generated Vite assets without removing the new maintained static files; verify a clean build serves only current assets and Git tracks the intended sources.
- [x] 3.3 Update `README.md` build, start, test, and development instructions to describe the WebJar workflow and `-DskipTests`; verify the documented commands and browser prerequisites.

## 4. Integration verification

- [x] 4.1 Run the default Maven test and clean package lifecycle with Node.js and npm unavailable and confirm all frontend and existing backend tests pass; separately verify `-DskipTests` builds without running tests.
- [x] 4.2 Run the packaged jar and verify `/`, the referenced WebJar JavaScript, and `/api/sudoku` work on one origin without a frontend server or CDN; exercise a generate-and-solve flow in a browser.
- [x] 4.3 Verify `spring-boot:run` serves changes to frontend resources without Node.js or npm and record any required refresh or restart steps in the development documentation.

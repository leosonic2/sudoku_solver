# Proposal

## Why

The application is already delivered as one Spring Boot jar, but building and testing it currently installs Node.js and npm to compile the Vue frontend. The frontend should be developed, tested, built, and served without Node.js or npm while keeping the existing single-application experience.

## What Changes

- Replace the frontend Maven plugin and the npm-based Vue toolchain with a pinned Vue 3 WebJar dependency.
- Rewrite the existing Vue single-file components and TypeScript as browser-executable HTML, CSS, and JavaScript, preserving the current gameplay, accessibility, API interactions, and error handling.
- Package the maintained frontend resources with Spring Boot and serve Vue locally from the WebJar, without a separate frontend server or CDN.
- Replace the Vitest coverage with Node-free automated frontend regression tests and document their browser prerequisites.
- Update build and development documentation and resource ignore rules; remove obsolete frontend toolchain files.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `application-delivery`: Replace the required frontend source location and require Node-free development, testing, packaging, and delivery using Vue from a Maven WebJar.

## Impact

- Affects `pom.xml`, frontend sources and tests, static resource handling, `.gitignore`, and `README.md`.
- The existing Sudoku API, its payloads, and the gameplay requirements remain unchanged.
- Automated UI regression tests will require a headless browser and compatible driver; they must not require Node.js or npm.

# Spec Delta

## Purpose

Package and deliver the interface and Sudoku API as one deployable application with a Java 25 runtime.

## ADDED Requirements

### Requirement: Embedded frontend and single artifact
The Vue frontend source SHALL reside within the Spring Boot project at `src/main/frontend`. A Maven package build SHALL produce a single runnable jar containing both the frontend assets and backend API.

#### Scenario: Package and run
- **WHEN** the project is built with a Java 25 JDK and the resulting jar is launched
- **THEN** the same application serves the UI at `/` and the Sudoku API at `/api/sudoku` without a separate frontend server.

### Requirement: Java 25 compatibility
The application SHALL build targeting Java release 25 and run on Java 25 using a compatible Spring Boot release.

#### Scenario: Java 25 build and runtime
- **WHEN** the application is built with JDK 25 and run on Java 25
- **THEN** it starts successfully and serves the application.

### Requirement: Same-origin frontend API requests
The packaged frontend SHALL address the Sudoku API via relative same-origin paths.

#### Scenario: Frontend sends a request
- **WHEN** a user generates or solves a puzzle from the packaged UI
- **THEN** the browser sends the request to `/api/sudoku` on the same origin.

### Requirement: Stateless concurrent requests
The backend SHALL keep game state client-side and isolate mutable solver state across requests.

#### Scenario: Concurrent games
- **WHEN** multiple clients generate or solve puzzles concurrently
- **THEN** each response reflects only its own request and no client game state is stored in the backend.

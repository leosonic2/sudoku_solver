# application-delivery Specification

## Purpose

Package and deliver the interface and Sudoku API as one deployable application with a Java 25 runtime.

## Requirements

### Requirement: Embedded frontend and single artifact
The Vue frontend source SHALL reside within the Spring Boot project's static resources. A Maven package build SHALL produce a single runnable jar containing both the frontend assets and backend API.

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

### Requirement: Node-free application workflow
Developing the frontend, running application tests, packaging, and starting the application SHALL NOT require or invoke Node.js or npm.

#### Scenario: Build and run without Node.js
- **WHEN** a developer runs the Maven test and package lifecycle or starts the Spring Boot application without Node.js or npm installed, with the documented build and test prerequisites available
- **THEN** the application builds, tests, and starts without installing or invoking either tool.

#### Scenario: Edit the frontend without a toolchain
- **WHEN** a developer changes the frontend source
- **THEN** the changes can be packaged and served by the Spring Boot application without Node.js, npm, or a separate frontend server.

### Requirement: Locally served Vue runtime
The application SHALL obtain its Vue runtime from a version-pinned Maven WebJar and serve it from the application origin.

#### Scenario: Open the packaged interface
- **WHEN** a browser opens the application with no access to external frontend CDNs
- **THEN** it loads Vue and the application assets from the same application and displays the interactive interface.

### Requirement: Node-free frontend regression tests
The normal Maven test lifecycle SHALL run automated frontend regression tests without Node.js or npm and report missing required test infrastructure as a failure rather than silently skipping the tests.

#### Scenario: Run frontend regression tests
- **WHEN** the required headless browser and compatible driver are available and Maven tests are enabled
- **THEN** automated tests exercise gameplay, accessibility, API interaction, and error handling against the frontend.

#### Scenario: Required test infrastructure is unavailable
- **WHEN** Maven tests are enabled but the required browser or driver is unavailable
- **THEN** the test run fails with an actionable error instead of reporting success without running frontend regression tests.

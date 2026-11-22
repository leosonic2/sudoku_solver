# Sudoku solver

Sudoku generator and player with an Apache Commons Math LP and branch-and-bound solver.
The Vue interface and REST API are packaged into one Spring Boot application.

## Requirements and build

- JDK 25; set `JAVA_HOME` to the JDK.
- Internet access on the first build for Maven dependencies.
- For frontend tests, Google Chrome or Chromium and a compatible ChromeDriver.
  Selenium Manager can download a driver on first use, which requires internet
  access. Alternatively, pass the path to an installed driver with
  `-Dwebdriver.chrome.driver=C:\path\to\chromedriver.exe`. Tests fail with an
  explanation if the browser or driver is unavailable; `-DskipTests` skips them.

On Windows, from the project root:

```powershell
.\mvnw.cmd clean package
java -jar target\sudoku-solver.jar
```

Open `http://localhost:8080`. Maven 3.6.3 or later can also be used with
`mvn clean package`. Maven packages the browser-ready frontend and the Vue
WebJar, and runs backend and frontend tests. Node.js and npm are not required.
Run just the tests with:

```powershell
.\mvnw.cmd test
```

`-DskipTests` skips the tests but still packages the frontend:

```powershell
.\mvnw.cmd package -DskipTests
```

Run directly from source with `.\mvnw.cmd spring-boot:run`. To serve edits to
frontend resources directly during development, use:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.addResources=true
```

Edit the files in `src\main\resources\static` and refresh the browser. Java
changes require rebuilding and restarting the application.

## Limits

All limits must be positive. Override them with Spring Boot properties, for
example:

```powershell
java -jar target\sudoku-solver.jar --sudoku.solver.timeout-ms=30000 --sudoku.solver.max-nodes=10000 --sudoku.solver.max-iterations=100000 --sudoku.generator.timeout-ms=10000
```

| Property | Default | Meaning |
| --- | ---: | --- |
| `sudoku.solver.timeout-ms` | 30000 | Wall-clock limit for a solver search, including worker startup. |
| `sudoku.solver.max-nodes` | 10000 | Maximum branch-and-bound nodes per search. |
| `sudoku.solver.max-iterations` | 100000 | Maximum simplex iterations per relaxation. |
| `sudoku.generator.timeout-ms` | 10000 | Shared wall-clock budget for a generation request and its uniqueness checks. |

An inconclusive uniqueness check is never treated as proof. Generation returns
the last proven unique puzzle if a later check exceeds its budget; if no
verified removal exists, it returns a limit error. Solver workers are terminated
when their wall-clock deadline expires.

## API

Both endpoints use JSON and relative paths on the application origin. Sizes
are integers from 2 through 16; boards are square arrays with integers from 0
through N, where 0 is empty. `boxRows` is the largest divisor of N not greater
than its square root, and `boxCols` is N divided by `boxRows`. For example, 6
uses 2-by-3 regions. Prime sizes use 1-by-N regions and form Latin squares.

### `POST /api/sudoku/generate`

Request:

```json
{"size":4}
```

An example response (the solution is never included):

```json
{"size":4,"boxRows":2,"boxCols":2,"puzzle":[[0,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]]}
```

### `POST /api/sudoku/solve`

`puzzle` contains the original clues; `current` contains those same clues
plus any player entries, which are also fixed during solving.

```json
{"size":4,"boxRows":2,"boxCols":2,"puzzle":[[1,0,0,4],[0,4,1,0],[0,1,4,0],[4,0,0,1]],"current":[[1,0,0,4],[0,4,1,0],[0,1,4,0],[4,0,0,1]]}
```

Example response:

```json
{"solution":[[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]],"solveTimeMs":152,"nodesExplored":1}
```

`solveTimeMs` is elapsed milliseconds; `nodesExplored` counts explored solver
nodes. Both are nonnegative.

### Errors

Errors contain string fields `code` and `message`, for example:

```json
{"code":"INFEASIBLE","message":"Your current entries make the puzzle unsolvable"}
```

| HTTP | Code | Meaning |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | Invalid JSON, size, region dimensions, board shape or values, or changed/cleared clue. |
| 422 | `INFEASIBLE` | The current entries have no valid completion. |
| 504 | `LIMIT_REACHED` | A solver limit was reached, or generation could not prove a usable puzzle in time. |
| 500 | `INTERNAL_ERROR` | An unexpected internal failure occurred. |

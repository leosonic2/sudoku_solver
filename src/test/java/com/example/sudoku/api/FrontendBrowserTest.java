package com.example.sudoku.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FrontendBrowserTest.FixtureController.class)
class FrontendBrowserTest {
    @LocalServerPort
    int port;

    WebDriver browser;

    @BeforeEach
    void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        try {
            browser = new ChromeDriver(options);
        } catch (WebDriverException | IllegalStateException cause) {
            throw new IllegalStateException(
                    "Frontend tests require Chrome or Chromium and a compatible ChromeDriver. "
                            + "Install them or allow Selenium Manager to provision a driver.", cause);
        }
    }

    @AfterEach
    void stopBrowser() {
        if (browser != null) {
            browser.quit();
        }
    }

    @Test
    void loadsSameOriginFixtureInBrowser() {
        browser.get("http://localhost:" + port + "/browser-probe.html");
        var result = new WebDriverWait(browser, Duration.ofSeconds(10))
                .until(driver -> {
                    var element = driver.findElement(By.id("result"));
                    return "sudoku-fixture".equals(element.getText()) ? element : null;
                });
        assertEquals("sudoku-fixture", result.getText());
    }

    @Test
    void loadsApplicationWithVueFromWebJar() {
        browser.get("http://localhost:" + port + "/");
        var heading = new WebDriverWait(browser, Duration.ofSeconds(10))
                .until(driver -> driver.findElement(By.tagName("h1")));
        assertEquals("Sudoku", heading.getText());
        assertEquals("Generate", browser.findElement(By.cssSelector("form button")).getText());
    }

    @Test
    void validatesSizeAndCanCancelReplacingGame() {
        openWithFixtures();
        setSize("17");
        button("Generate").click();
        assertTrue(waitForText("[role=alert]", "Choose an integer from 2 to 16.").isDisplayed());
        assertEquals(0L, script("return window.requests.length"));
        setSize("5");
        assertTrue(waitForText("form", "Latin square").isDisplayed());
        setSize("4");
        button("Generate").click();
        waitForText("section", "4-by-4 puzzle ready.");
        assertEquals("/api/sudoku/generate", script("return window.requests[0].url"));
        script("window.confirmResult = false");
        button("Generate").click();
        assertEquals(1L, script("return window.requests.length"));
        assertEquals("1", cell(0, 0).getText());
    }

    @Test
    void preservesCluesNavigationConflictsAndReset() {
        openWithFixtures();
        generateFour();
        WebElement clue = cell(0, 0);
        clue.click();
        clue.sendKeys("2");
        assertEquals("1", clue.getText());
        clue.sendKeys(Keys.ARROW_RIGHT);
        waitFor(() -> "0:1".equals(browser.switchTo().activeElement().getAttribute("data-cell")));
        WebElement entry = cell(0, 1);
        assertTrue(entry.getAttribute("aria-label").contains("Row 1, column 2"));
        assertTrue(cell(1, 0).getAttribute("class").contains("related"));
        assertFalse(cell(3, 3).getAttribute("class").contains("related"));
        assertTrue(cell(0, 2).getAttribute("class").contains("box-left"));
        assertTrue(cell(2, 0).getAttribute("class").contains("box-top"));
        number(4).click();
        waitFor(() -> "true".equals(entry.getAttribute("aria-invalid")));
        assertTrue(waitForText("section", "Conflicting entries").isDisplayed());
        button("Reset").click();
        waitFor(() -> entry.getText().isEmpty());
        assertEquals("1", clue.getText());
    }

    @Test
    void supportsMultiDigitKeyboardEntryAndClearing() {
        openWithFixtures();
        script("""
                window.responses['/api/sudoku/generate'].body = {
                  size: 12, boxRows: 3, boxCols: 4,
                  puzzle: Array.from({length: 12}, () => Array(12).fill(0))
                }
                """);
        setSize("12");
        button("Generate").click();
        waitForText("section", "12-by-12 puzzle ready.");
        WebElement entry = cell(0, 0);
        entry.click();
        entry.sendKeys("12");
        waitFor(() -> "12".equals(entry.getText()));
        entry.sendKeys(Keys.BACK_SPACE);
        waitFor(() -> entry.getText().isEmpty());
    }

    @Test
    void solvesCurrentBoardAndShowsPendingState() {
        openWithFixtures();
        generateFour();
        cell(0, 1).click();
        number(2).click();
        script("window.responses['/api/sudoku/solve'].pending = true");
        button("Solve Automatically").click();
        waitForText("[role=status]", "Solving puzzle...");
        assertFalse(button("Generate").isEnabled());
        assertFalse(button("Reset").isEnabled());
        assertFalse(button("Solve Automatically").isEnabled());
        assertEquals(2L, script("return window.requests.at(-1).body.current[0][1]"));
        script("window.resolvePending()");
        waitForText("section", "Puzzle solved automatically in 42 ms.");
        assertEquals("3", cell(0, 2).getText());
        assertTrue(cell(0, 2).getAttribute("class").contains("solver-filled"));
        assertFalse(cell(0, 1).getAttribute("class").contains("solver-filled"));
        assertTrue(button("Solve Automatically").isEnabled());
    }

    @Test
    void reportsInvalidAndFailedResponsesWithoutLosingEntries() {
        openWithFixtures();
        setSize("4");
        script("window.responses['/api/sudoku/generate'] = {ok: false, status: 504, body: {message: 'Generation timed out'}}");
        button("Generate").click();
        waitForText("[role=alert]", "Generation timed out");
        script("window.responses['/api/sudoku/generate'] = {ok: true, status: 200, body: {size: 4}}");
        button("Generate").click();
        waitForText("[role=alert]", "The server returned an invalid puzzle.");
        script("window.responses['/api/sudoku/generate'] = window.validPuzzle()");
        button("Generate").click();
        waitForText("section", "4-by-4 puzzle ready.");
        cell(0, 1).click();
        number(2).click();
        script("window.responses['/api/sudoku/solve'] = {ok: false, status: 422, body: {message: 'Unsolvable entries'}}");
        button("Solve Automatically").click();
        waitForText("[role=alert]", "Unsolvable entries");
        assertEquals("2", cell(0, 1).getText());
        assertTrue(button("Reset").isEnabled());
        script("window.responses['/api/sudoku/solve'] = {ok: true, status: 200, body: {solution: [[1]], solveTimeMs: 1, nodesExplored: 1}}");
        button("Solve Automatically").click();
        waitForText("[role=alert]", "The server returned an invalid solution.");
        assertEquals("2", cell(0, 1).getText());
    }

    @Test
    void recognizesManuallyCompletedBoard() {
        openWithFixtures();
        generateFour();
        int[][] solution = {{1, 2, 3, 4}, {3, 4, 1, 2}, {2, 1, 4, 3}, {4, 3, 2, 1}};
        int[][] puzzle = {{1, 0, 0, 4}, {0, 4, 1, 0}, {0, 1, 4, 0}, {4, 0, 0, 1}};
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                if (puzzle[row][column] == 0) {
                    cell(row, column).click();
                    number(solution[row][column]).click();
                }
            }
        }
        waitForText("section", "Puzzle solved!");
    }

    private void openWithFixtures() {
        browser.get("http://localhost:" + port + "/");
        waitForText("h1", "Sudoku");
        script("""
                window.requests = [];
                window.confirmResult = true;
                window.confirm = () => window.confirmResult;
                window.validPuzzle = () => ({ok: true, status: 200, body: {
                  size: 4, boxRows: 2, boxCols: 2,
                  puzzle: [[1,0,0,4],[0,4,1,0],[0,1,4,0],[4,0,0,1]]
                }});
                window.responses = {
                  '/api/sudoku/generate': window.validPuzzle(),
                  '/api/sudoku/solve': {ok: true, status: 200, body: {
                    solution: [[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]],
                    solveTimeMs: 42, nodesExplored: 1
                  }}
                };
                window.fetch = (url, options) => {
                  window.requests.push({url, body: JSON.parse(options.body)});
                  const fixture = window.responses[url];
                  const response = () => ({ok: fixture.ok, status: fixture.status, json: async () => fixture.body});
                  return fixture.pending
                    ? new Promise(resolve => { window.resolvePending = () => resolve(response()) })
                    : Promise.resolve(response());
                };
                """);
    }

    private Object script(String javascript) {
        return ((JavascriptExecutor) browser).executeScript(javascript);
    }

    private void setSize(String value) {
        WebElement input = browser.findElement(By.id("size"));
        input.clear();
        input.sendKeys(value);
    }

    private void generateFour() {
        setSize("4");
        button("Generate").click();
        waitForText("section", "4-by-4 puzzle ready.");
    }

    private WebElement button(String text) {
        return browser.findElement(By.xpath("//button[normalize-space(.)='" + text + "']"));
    }

    private WebElement number(int value) {
        return browser.findElement(By.cssSelector(".number-pad button:nth-child(" + value + ")"));
    }

    private WebElement cell(int row, int column) {
        return browser.findElement(By.cssSelector("[data-cell='" + row + ":" + column + "']"));
    }

    private WebElement waitForText(String selector, String text) {
        return new WebDriverWait(browser, Duration.ofSeconds(10)).until(driver -> {
            WebElement element = driver.findElement(By.cssSelector(selector));
            return element.getText().contains(text) ? element : null;
        });
    }

    private void waitFor(java.util.function.BooleanSupplier condition) {
        new WebDriverWait(browser, Duration.ofSeconds(10)).until(driver -> condition.getAsBoolean());
    }

    @RestController
    static class FixtureController {
        @GetMapping("/browser-test/fixture")
        Map<String, String> fixture() {
            return Map.of("value", "sudoku-fixture");
        }
    }
}

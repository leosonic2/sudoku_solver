# Design

## Context

The current Maven build installs Node and npm, runs Vitest, and invokes Vite to generate resources under `src/main/resources/static`, which is ignored by Git. The Vue application is written as TypeScript single-file components; the API uses same-origin paths. See `proposal.md` for the motivation and the `application-delivery` delta for the delivery contract.

## Goals / Non-Goals

**Goals:**
- Keep the Spring Boot jar as the only deployed application and retain the current UI and API behavior.
- Make frontend source and automated verification independent of Node and npm.
- Keep frontend regression tests in the normal Maven test lifecycle.

**Non-Goals:**
- Changing Sudoku generation, solving, API contracts, or gameplay requirements.
- Supporting Vue single-file components, TypeScript compilation, or Vite hot reload without their toolchain.
- Removing unrelated optional tools used for OpenSpec or repository automation.

## Decisions

### Serve a pinned Vue WebJar

Replace `frontend-maven-plugin` with `org.webjars.npm:vue`, pinned to the existing Vue version `3.5.46`. Load the full browser ESM build, including the template compiler, via a versioned same-origin WebJar URL. Spring Boot serves the dependency's resources; no CDN, standalone frontend server, npm installation, or vendored Vue copy is needed. Verify the actual WebJar resource path and content type from both the application and packaged jar.

The versioned URL and Maven dependency must stay synchronized; an automated resource-loading test must catch mismatches. A CDN would add a runtime network dependency, while retaining Vite or prebuilt bundles would leave Node in the frontend development workflow.

### Maintain browser-ready application sources

Move the maintained UI into `src/main/resources/static` as HTML, CSS, and JavaScript modules. Translate the existing components, game state, request validation, styling, and keyboard behavior to Vue component objects and browser-executable JavaScript. Keep the existing API requests relative to the application origin and preserve all gameplay requirements in `sudoku-gameplay`.

Adjust `.gitignore` so these source resources are tracked rather than ignored. Remove obsolete npm, TypeScript, Vite, and generated asset files only after their replacements are in place. Browser-ready modules avoid a compilation step; a WebJar alone cannot execute the current TypeScript and `.vue` files.

### Replace Vitest with Maven-driven browser tests

Use JUnit and test-scoped Selenium WebDriver with a headless browser and compatible driver to exercise the actual Vue application through an embedded Spring Boot server. Use deterministic test fixtures and controlled API responses where necessary to cover loading, failures, and malformed responses; retain the existing Java API and solver tests. The default Maven test lifecycle runs these tests, and `-DskipTests` retains its existing meaning. Missing browser or driver prerequisites must fail clearly rather than silently skipping coverage.

Document browser and driver installation or provisioning, including the first-run network requirements. Browser tests provide DOM, keyboard, accessibility attributes, and request coverage that Java-only API tests cannot; a Node-based runner would violate the goal. No JavaScript browser engine without proven Vue compatibility is assumed.

## Risks / Trade-offs

- **Browser and driver are new test prerequisites** -> Document supported setup and actionable failures; the runtime jar itself does not require either on the server.
- **Runtime template compilation differs from Vite's precompiled components** -> Use the full Vue browser build and run regression tests against the actual served application. A future restrictive content-security policy must account for runtime compilation.
- **JavaScript replaces TypeScript checks and scoped styles** -> Preserve validation and use focused browser tests; scope CSS selectors explicitly to avoid styling regressions.
- **Ignored static files or stale hashed Vite assets could mask missing source** -> Update ignore rules and validate from a clean checkout/build without relying on previously generated files.
- **WebJar version and URL can drift** -> Pin the dependency and test that the referenced runtime URL returns the expected JavaScript.

## Migration Plan

Introduce the WebJar and browser-ready UI, replace the frontend test coverage, and remove the old build toolchain and stale generated assets. Update the documentation and verify a clean Maven build plus the packaged jar. Rollback is a normal revert of the migration, restoring the prior frontend sources and Maven plugin together.

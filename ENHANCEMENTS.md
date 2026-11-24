# Enhancement notes

## Existing implementation retained

The original Java/Spring Boot architecture, PDFBox parser, role knowledge base, weighted scorer, semantic matcher, roadmap resources, and local Ollama integration remain in place.

## Changes in this pass

- Fixed the frontend/backend upload route mismatch. The form now posts to `/analyze`; `/upload` remains as a compatibility alias.
- Added the model attributes the existing Thymeleaf results page expects (`score`, `level`, `verdict`, `matched`, `missing`, and `skills`).
- Updated role selection to use `RoleInfo.name()` rather than rendering the whole record as the role value.
- Added a transparent weighted-score breakdown and a skill-evidence matrix.
- Added an asynchronous AI coach panel using the existing `/coach` service.
- Added responsive styles for the score breakdown, evidence matrix, and coach suggestions.
- Added graceful fallbacks for unavailable chat and embedding calls where deterministic or exact-key behavior is possible.
- Added server-side upload size and filename checks, sanitized the displayed filename, and replaced raw exception messages with user-safe errors.
- Replaced the application-context-only test with focused unit tests for role parsing, resume parsing, and score thresholds.
- Added a practical `README.md` for local setup and execution.

## Verification performed

- JavaScript syntax check passed with Node.js.
- All seven result-tab targets have matching panel IDs.
- Basic Java source brace-balance smoke checks passed.
- Maven tests could not be executed in this environment: the available runtime is Java 21 while the project configures Java 25, and the Maven wrapper could not download Maven because the environment could not reach Maven Central.

Run `.\mvnw.cmd test` from PowerShell on a machine with Java 25 and Maven Central access to perform the full build and test verification.

# AI Resume Analyzer

A Spring Boot application that analyzes a PDF resume against a selected career role. It combines deterministic resume parsing and weighted skill scoring with optional local Ollama chat and embedding models.

## Features

- PDF upload and text extraction with Apache PDFBox (5 MB limit).
- Rule-based resume structure and health checks.
- Role-specific core and bonus skill matching.
- Weighted readiness score with a transparent score breakdown.
- Evidence levels: proven, listed, AI-inferred, and missing.
- Related-skill hints using embeddings.
- Personalized learning roadmap from the local role and learning knowledge bases.
- AI-assisted bullet-point coaching.
- Dark/light theme, responsive results tabs, roadmap progress, copy controls, and print-to-PDF.

## Requirements

- Java 25 (the version configured in `pom.xml`).
- Ollama running locally at `http://localhost:11434`.
- The Ollama models configured in `src/main/resources/application.properties`:
  - `llama3.2:3b` for chat and skill extraction.
  - `nomic-embed-text` for embeddings and semantic matching.

## Run locally on Windows PowerShell

Start Ollama, then make sure the models are available:

```powershell
ollama pull llama3.2:3b
ollama pull nomic-embed-text
```

From the project root:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080`.

On macOS/Linux, use `./mvnw test` and `./mvnw spring-boot:run`.

## How analysis works

1. The uploaded PDF is parsed in memory; the application does not persist the uploaded resume.
2. The parser identifies resume sections, contact details, bullets, and structural checks.
3. Skills explicitly listed by the candidate are separated from skills inferred by the local chat model.
4. The scorer compares evidence against the selected role's skill definitions in `src/main/resources/roles/`.
5. Embedding-based related-skill hints are optional and do not change the score.
6. Roadmap steps are assembled from `src/main/resources/learning/resources.txt` and `src/main/resources/roadmaps/`.
7. The AI coach rewrites up to three supplied bullets and is instructed not to invent achievements or metrics.

## Configuration

Update `src/main/resources/application.properties` to change the Ollama URL, model names, upload limits, or semantic similarity threshold. Role definitions, roadmap content, and learning resources are plain-text files under `src/main/resources/`.

## Limitations

- Scanned/image-only PDFs need OCR; this project currently extracts embedded PDF text.
- AI and embedding results depend on the configured local Ollama models. If chat or embedding requests fail, deterministic parsing and exact-key learning-resource lookup remain available where possible.
- The readiness score is a guidance signal based on the role knowledge base, not a hiring prediction.

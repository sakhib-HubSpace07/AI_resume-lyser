# AI Resume Analyzer

```{=html}
<p align="center">
```
`<strong>`{=html}Understand your resume. Measure role readiness. Build a
practical learning plan.`</strong>`{=html}
```{=html}
</p>
```
```{=html}
<p align="center">
```
A local-first, AI-assisted resume analysis application built with Java,
Spring Boot, Thymeleaf, Apache PDFBox, Spring AI and Ollama.
```{=html}
</p>
```
```{=html}
<p align="center">
```
`<a href="https://github.com/sakhib-HubSpace07/AI_resume-analyzer">`{=html}GitHub
Repository`</a>`{=html} ·
`<a href="https://github.com/sakhib-HubSpace07/AI_resume-analyzer/issues">`{=html}Report
an Issue`</a>`{=html} ·
`<a href="https://github.com/sakhib-HubSpace07/AI_resume-analyzer/graphs/contributors">`{=html}Contributors`</a>`{=html}
```{=html}
</p>
```

------------------------------------------------------------------------

## Table of Contents

-   [1. About the Project](#1-about-the-project)
-   [2. The Problem](#2-the-problem)
-   [3. Project Goals](#3-project-goals)
-   [4. Key Features](#4-key-features)
-   [5. Application Architecture](#5-application-architecture)
-   [6. Complete Resume Analysis
    Workflow](#6-complete-resume-analysis-workflow)
-   [7. How the Readiness Score Works](#7-how-the-readiness-score-works)
-   [8. Skill Evidence Classification](#8-skill-evidence-classification)
-   [9. Personalized Learning Roadmap](#9-personalized-learning-roadmap)
-   [10. AI Resume Coach](#10-ai-resume-coach)
-   [11. Supported Career Roles](#11-supported-career-roles)
-   [12. Technology Stack](#12-technology-stack)
-   [13. Project Folder Structure](#13-project-folder-structure)
-   [14. Prerequisites](#14-prerequisites)
-   [15. Installation and Setup](#15-installation-and-setup)
-   [16. Running the Application](#16-running-the-application)
-   [17. Using the Application](#17-using-the-application)
-   [18. Configuration](#18-configuration)
-   [19. Privacy and Data Handling](#19-privacy-and-data-handling)
-   [20. Testing](#20-testing)
-   [21. Current Limitations and Future
    Improvements](#21-current-limitations-and-future-improvements)
-   [22. Troubleshooting](#22-troubleshooting)
-   [23. Contributing](#23-contributing)
-   [24. License](#24-license)
-   [25. Developer and Contact](#25-developer-and-contact)

------------------------------------------------------------------------

## 1. About the Project

**AI Resume Analyzer** is a web application that evaluates a PDF resume
against a selected career role. It combines deterministic Java-based
resume parsing and role-specific scoring with locally hosted AI models
through Ollama.

Instead of returning only a generic score, the application explains:

-   Which role-related skills are supported by the resume.
-   Which skills are mentioned but not demonstrated through work.
-   Which skills may be inferred from project or experience descriptions
    by the local language model.
-   Which required skills are not currently evidenced.
-   How the resume performs across weighted readiness dimensions.
-   Which learning resources, advanced topics, projects and career steps
    can help close the gaps.
-   How selected resume bullet points could be written more effectively
    without inventing achievements.

The application is designed as a **career-readiness guide**, not as an
automated hiring decision system.

## 2. The Problem

Job seekers often face several challenges when preparing a resume for a
particular role:

1.  They may not know which technical skills a target role expects.
2.  A resume can list technologies without showing where or how they
    were used.
3.  Generic resume scanners often provide a score without explaining the
    evidence behind it.
4.  Candidates may know what they are missing but not what to learn
    next.
5.  Experience bullet points may describe responsibilities without
    communicating measurable impact.

This project brings these tasks into one workflow: **select a role →
upload a resume → understand the evidence → review the gaps → follow a
learning roadmap → improve bullet points.**

## 3. Project Goals

-   Provide role-specific rather than one-size-fits-all resume analysis.
-   Distinguish skills demonstrated in experience/projects from skills
    merely listed.
-   Make the score understandable through explicit dimensions and
    weights.
-   Use local AI models for selected language and embedding tasks.
-   Generate actionable learning guidance from editable project
    knowledge files.
-   Keep the uploaded PDF in the application's request-processing flow
    rather than saving it as an application file.
-   Offer a clear, responsive interface for reviewing results.

## 4. Key Features

  -----------------------------------------------------------------------
  Feature                             What it does
  ----------------------------------- -----------------------------------
  PDF resume upload                   Accepts PDF files up to 5 MB and
                                      extracts embedded text using
                                      PDFBox.

  Resume structure analysis           Identifies contact details, common
                                      sections, bullets,
                                      experience/project blocks and
                                      structural checks.

  Role selection                      Compares the resume against one of
                                      the role definitions shipped with
                                      the project.

  Skill extraction                    Reads skills from the resume and
                                      asks the local chat model to
                                      identify skills described in
                                      relevant work sections.

  Evidence-aware matching             Classifies each role skill as
                                      proven, listed, AI-inferred or
                                      missing.

  Weighted readiness score            Calculates a score from skill
                                      match, evidence, measurable impact
                                      and resume structure.

  Related-skill hints                 Uses text embeddings to suggest a
                                      potentially related existing skill
                                      for a missing skill. These hints do
                                      not change the score.

  Learning roadmap                    Builds a four-stage plan using
                                      role-specific roadmap entries and
                                      learning resources.

  AI bullet-point coach               Suggests clearer experience/project
                                      bullets while instructing the model
                                      not to invent facts or metrics.

  Results dashboard                   Presents overview, roadmap, resume
                                      breakdown, skill match, detected
                                      skills, AI coach and extracted raw
                                      text.

  Interface preferences               Includes dark/light theme
                                      switching, responsive layouts,
                                      result tabs, copy controls, roadmap
                                      progress interactions and
                                      print-friendly styling.
  -----------------------------------------------------------------------

## 5. Application Architecture

The application follows a layered Spring Boot design. The browser
communicates with the Spring MVC controller. The controller coordinates
focused services, while role and learning knowledge are loaded from text
resources packaged with the application.

### 5.1 High-level architecture

``` mermaid
flowchart TB
    U["User"]
    UI["Browser UI<br/>Thymeleaf + HTML + CSS + JavaScript"]
    C["ResumeController<br/>Spring MVC"]
    PDF["Apache PDFBox<br/>PDF text extraction"]
    RP["ResumeParser<br/>Sections, contacts, bullets, checks"]
    SE["SkillExtractor<br/>Listed + AI-inferred skills"]
    SC["ResumeScorer<br/>Evidence + weighted score"]
    SM["SemanticMatcher<br/>Embedding similarity hints"]
    RG["RoadmapGenerator<br/>Personalized 4-phase plan"]
    RC["ResumeCoach<br/>Bullet point suggestions"]
    RKB[("Role Knowledge Base<br/>src/main/resources/roles")]
    LKB[("Learning Resources<br/>resources.txt")]
    RMAP[("Roadmap Knowledge Base<br/>src/main/resources/roadmaps")]
    OLL["Local Ollama Service<br/>llama3.2:3b"]
    EMB["Local Embedding Model<br/>nomic-embed-text"]
    V["Results returned to browser"]

    U --> UI --> C
    C --> PDF --> RP
    C --> SE
    C --> SC
    C --> SM
    C --> RG
    C --> RC
    RKB --> SC
    RKB --> RG
    LKB --> RG
    RMAP --> RG
    SE <--> OLL
    RG <--> OLL
    RC <--> OLL
    SM <--> EMB
    RG <--> EMB
    RP --> C
    SE --> C
    SC --> C
    SM --> C
    RG --> C
    RC --> C
    C --> V --> UI
```

**How to read the diagram:** the controller coordinates the analysis.
PDFBox extracts text; Java services structure and evaluate it. Local
Ollama models support selected AI tasks. Plain-text role, roadmap and
learning files supply the domain knowledge used by the services.

### 5.2 Main application layers

  ------------------------------------------------------------------------------
  Layer                   Main components                Responsibility
  ----------------------- ------------------------------ -----------------------
  Presentation            `index.html`, `style.css`,     Upload form, role
                          `app.js`                       selection, results
                                                         dashboard and browser
                                                         interactions.

  Web/controller          `ResumeController.java`        Handles page rendering,
                                                         resume analysis
                                                         requests and coach
                                                         requests.

  PDF processing          `ResumeParser.java`, PDFBox    Extracts and structures
                                                         text from uploaded
                                                         PDFs.

  Skill intelligence      `SkillExtractor.java`,         Identifies
                          `SemanticMatcher.java`         listed/AI-inferred
                                                         skills and
                                                         related-skill hints.

  Evaluation              `ResumeScorer.java`            Evaluates role skill
                                                         evidence and calculates
                                                         the readiness score.

  Career guidance         `RoadmapGenerator.java`,       Creates learning steps
                          `ResumeCoach.java`             and improves selected
                                                         resume bullets.

  Knowledge/data          `RoleKnowledgeBase.java`,      Loads role definitions,
                          `RoadmapKnowledgeBase.java`,   roadmap entries and
                          `LearningKnowledgeBase.java`   learning resources.

  AI integration          Spring AI + Ollama             Connects application
                                                         services to local chat
                                                         and embedding models.
  ------------------------------------------------------------------------------

### 5.3 Request and response sequence

``` mermaid
sequenceDiagram
    actor User
    participant Browser
    participant Controller as ResumeController
    participant PDF as PDFBox
    participant Parser as ResumeParser
    participant Extractor as SkillExtractor
    participant Scorer as ResumeScorer
    participant Matcher as SemanticMatcher
    participant Roadmap as RoadmapGenerator
    participant Ollama as Local Ollama

    User->>Browser: Select role and PDF
    Browser->>Controller: POST /analyze
    Controller->>PDF: Read uploaded PDF bytes
    PDF-->>Controller: Extracted text + page count
    Controller->>Parser: Parse resume text
    Parser-->>Controller: Structured resume
    Controller->>Extractor: Extract listed and inferred skills
    Extractor->>Ollama: Request skill extraction
    Ollama-->>Extractor: Candidate skill list
    Extractor-->>Controller: Extracted skills
    Controller->>Scorer: Compare resume with selected role
    Scorer-->>Controller: Evidence statuses + score
    Controller->>Matcher: Find related skills for gaps
    Matcher->>Ollama: Request embeddings
    Ollama-->>Matcher: Vectors
    Matcher-->>Controller: Related-skill hints
    Controller->>Roadmap: Build learning roadmap
    Roadmap->>Ollama: Request short roadmap summary
    Ollama-->>Roadmap: Summary
    Roadmap-->>Controller: Roadmap phases and steps
    Controller-->>Browser: Render analysis results
    Browser-->>User: Display dashboard
```

The AI coach is a separate request: the browser posts bullet points to
`/coach`, and the controller returns the coach fragment for the results
page.

## 6. Complete Resume Analysis Workflow

The main analysis endpoint accepts the selected role and uploaded PDF.
The current controller supports both `/analyze` and `/upload` for this
operation.

``` mermaid
flowchart TD
    A["Start: user selects target role"] --> B["Upload PDF (maximum 5 MB)"]
    B --> C{"Role valid and file acceptable?"}
    C -- No --> E["Show validation message"]
    C -- Yes --> D["Extract text with PDFBox"]
    D --> F["Parse resume structure"]
    F --> G["Extract listed skills"]
    G --> H["Ask local Llama model for skills in work descriptions"]
    H --> I["Compare skills with selected role"]
    I --> J["Assign evidence status"]
    J --> K["Calculate weighted score"]
    K --> L["Find optional related-skill hints"]
    L --> M["Build learning roadmap"]
    M --> N["Prepare resume checks and coach input"]
    N --> O["Render results dashboard"]
    E --> P["End / user can try again"]
    O --> P
```

### Step-by-step explanation

1.  **Role validation:** the selected role must exist in the loaded role
    knowledge base.
2.  **File validation:** the file must not be empty, must have a `.pdf`
    filename and must not exceed 5 MB.
3.  **Text extraction:** Apache PDFBox reads the PDF bytes and extracts
    embedded text and page count.
4.  **Resume parsing:** the parser identifies likely name/contact
    details, headings, sections, bullet points, experience/project
    blocks and resume health checks.
5.  **Skill extraction:** explicit skills are read from parsed skill
    groups. The local chat model is asked to identify technologies
    clearly present in summary, experience and project text.
6.  **Role comparison:** each skill in the selected role definition is
    checked against the resume and extracted AI skill list.
7.  **Scoring:** core skills carry twice the weight of bonus skills.
    Evidence quality, measurable bullet points and structural checks
    also contribute.
8.  **Semantic hints:** the embedding model can identify a related skill
    already present in the resume for a missing requirement. This is
    advisory only.
9.  **Roadmap generation:** missing core skills, advanced topics,
    project ideas and career steps are assembled from the knowledge
    files.
10. **Results:** the controller places the analysis objects into the
    Thymeleaf model and renders the dashboard.

## 7. How the Readiness Score Works

The overall score is calculated from four dimensions. These weights are
implemented in `ResumeScorer.java`.

  ------------------------------------------------------------------------
  Dimension                                   Weight What it measures
  --------------------- ---------------------------- ---------------------
  Skills match                                   55% Match against the
                                                     selected role's core
                                                     and bonus skills.

  Evidence in                                    20% How many matched
  experience                                         skills are supported
                                                     by experience or
                                                     project evidence.

  Impact and numbers                             15% How many resume
                                                     bullet points contain
                                                     numbers, scaled
                                                     against the
                                                     implementation's
                                                     target ratio.

  Resume structure                               10% Percentage of
                                                     parser-defined resume
                                                     health checks passed.

  **Total**                                 **100%** Weighted readiness
                                                     score.
  ------------------------------------------------------------------------

The calculation is:

``` text
Overall Score =
    (Skills Match × 0.55)
  + (Evidence in Experience × 0.20)
  + (Impact and Numbers × 0.15)
  + (Resume Structure × 0.10)
```

### Skill match calculation

Each required role skill is assigned a status-based value:

  Evidence status     Points earned
  ----------------- ---------------
  Proven                        1.0
  Listed                        0.6
  AI-inferred                   0.4
  Missing                       0.0

Core skills have a multiplier of `2.0`; bonus skills have a multiplier
of `1.0`.

``` text
Skills Match (%) =
  (sum of skill weight × status points)
  ------------------------------------- × 100
       (sum of all skill weights)
```

### Score interpretation

      Score Verdict
  --------- ----------------------------
    85--100 Excellent match
     70--84 Strong match
     50--69 Fair match, with some gaps
      0--49 Needs work for this role

These are guidance labels defined by the application. They are not
industry-wide standards, job-offer probabilities or hiring predictions.

## 8. Skill Evidence Classification

The application deliberately distinguishes between a skill being named
and a skill being demonstrated.

  -----------------------------------------------------------------------
  Status                  Meaning                 How it is determined
  ----------------------- ----------------------- -----------------------
  **Proven**              The skill is found in   The role skill or alias
                          an experience or        matches text in the
                          project block.          parsed
                                                  experience/project
                                                  evidence.

  **Listed**              The skill appears in    The role skill matches
                          the resume, but no      the full resume text,
                          matching                but no evidence block
                          experience/project      is identified.
                          evidence is found.      

  **AI-inferred**         The local language      The skill appears in
                          model identifies the    the AI-extracted skill
                          skill in relevant       list.
                          resume content, but the 
                          direct full-text        
                          matching step did not   
                          find it.                

  **Missing**             No evidence for the     No direct or
                          skill is found.         AI-extracted match is
                                                  found.
  -----------------------------------------------------------------------

The scorer uses whole-word-style matching with aliases from the role
files. This helps avoid some accidental partial matches---for example,
matching `java` inside `javascript`.

**Important:** AI-inferred means the model found a possible skill
mention in the supplied work descriptions. It is not independent proof
of proficiency. Candidates should review all results for accuracy.

## 9. Personalized Learning Roadmap

The roadmap generator creates up to four types of learning phases,
omitting phases that have no steps.

``` mermaid
flowchart LR
    A["Resume analysis"] --> B["Phase 1<br/>Close your skill gaps"]
    B --> C["Phase 2<br/>Level up with advanced skills"]
    C --> D["Phase 3<br/>Prove it with projects"]
    D --> E["Phase 4<br/>Get job-ready"]
```

  ---------------------------------------------------------------------------
  Phase                   Purpose                 Content source
  ----------------------- ----------------------- ---------------------------
  1\. Close your skill    Prioritize missing core Role skill definitions and
  gaps                    skills and find         `learning/resources.txt`.
                          relevant learning       
                          guidance.               

  2\. Level up with       Identify additional     The selected role's roadmap
  advanced skills         role-specific topics.   file, `[advanced]` section.

  3\. Prove it with       Turn knowledge into     The selected role's roadmap
  projects                demonstrable portfolio  file, `[projects]` section.
                          work.                   

  4\. Get job-ready       Prepare for interviews  The selected role's roadmap
                          and career steps.       file, `[career]` section.
  ---------------------------------------------------------------------------

The application can use the embedding-backed learning resource store to
retrieve relevant entries. It also has exact-key/alias lookup as a
fallback. The local chat model writes a short summary; the roadmap steps
themselves are grounded in the project's local knowledge files.

The estimated learning duration is derived from durations included in
roadmap steps. It is an estimate, not a guaranteed completion time.

## 10. AI Resume Coach

The AI Coach is available through the `/coach` endpoint and the
dashboard's **AI coach** tab.

It accepts up to 5,000 characters and processes at most three non-empty
bullet points. The model is instructed to:

-   Start each rewritten bullet with a strong action verb.
-   Name the technology when it is present in the original information.
-   Emphasize the result or impact when the source bullet supports it.
-   Never invent tools, achievements or numerical results.
-   Use placeholders such as `[X%]` or `[N users]` when a metric would
    be useful but was not supplied.

Always verify the suggestions and replace placeholders with real,
supportable information before using them in a resume.

## 11. Supported Career Roles

The current project contains ten role-definition files and ten
corresponding roadmap files:

1.  AI Agent Engineer
2.  AI Security Engineer
3.  Cloud DevOps Engineer
4.  Computer Vision Engineer
5.  Data Analyst
6.  Full Stack AI Developer
7.  Generative AI Engineer
8.  Java Developer
9.  ML Engineer
10. MLOps Engineer

The exact skill names, aliases, categories, core/bonus labels, role
display metadata and roadmap content are stored in editable text files
under `src/main/resources/roles/` and `src/main/resources/roadmaps/`.

### Adding or editing a role

A role file uses this format:

``` text
Java Developer
@icon ☕
@tagline Build reliable backend services
@order 1

[Backend]
Java|core java
Spring Boot|spring
+Redis|redis cache

[Databases]
PostgreSQL|postgres|sql
+MongoDB|mongo
```

-   The first line is the role name.
-   `@icon`, `@tagline` and `@order` are optional display/sorting
    metadata.
-   A line in square brackets starts a skill category.
-   A normal skill line is a core skill.
-   A line beginning with `+` is a bonus skill.
-   Pipe-separated values are aliases used during matching.

Create a matching roadmap file with the same role name on its first line
and sections such as `[advanced]`, `[projects]` and `[career]`.

## 12. Technology Stack

  -----------------------------------------------------------------------
  Technology                          Use in this project
  ----------------------------------- -----------------------------------
  Java 25                             Application language; configured in
                                      Maven.

  Spring Boot 4.1.3                   Application framework and
                                      dependency management parent.

  Spring MVC                          HTTP request handling and
                                      controller endpoints.

  Thymeleaf                           Server-side HTML rendering and
                                      coach fragment rendering.

  Apache PDFBox 3.0.8                 PDF text extraction.

  Spring AI 2.0.1                     Integration with Ollama chat,
                                      embeddings and vector-store APIs.

  Ollama                              Runs configured AI models locally.

  Llama 3.2 3B (`llama3.2:3b`)        Chat tasks such as skill
                                      extraction, roadmap summary and
                                      bullet coaching.

  Nomic Embed Text                    Text embeddings for semantic
  (`nomic-embed-text`)                matching and learning-resource
                                      retrieval.

  HTML5, CSS3, JavaScript             Frontend structure, responsive
                                      styling and browser interactions.

  Maven Wrapper                       Reproducible Maven command entry
                                      point.

  JUnit 5 / Spring Boot test          Automated tests.
  dependencies                        
  -----------------------------------------------------------------------

## 13. Project Folder Structure

``` text
resumeanalyzer/
│
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
│
├── src/
│   ├── main/
│   │   ├── java/com/resumeai/resumeanalyzer/
│   │   │   ├── ResumeanalyzerApplication.java
│   │   │   ├── ResumeController.java
│   │   │   ├── ResumeParser.java
│   │   │   ├── SkillExtractor.java
│   │   │   ├── ResumeScorer.java
│   │   │   ├── SemanticMatcher.java
│   │   │   ├── RoadmapGenerator.java
│   │   │   ├── ResumeCoach.java
│   │   │   ├── RoleKnowledgeBase.java
│   │   │   ├── RoadmapKnowledgeBase.java
│   │   │   ├── LearningKnowledgeBase.java
│   │   │   └── TestController.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── roles/
│   │       │   └── *.txt
│   │       ├── roadmaps/
│   │       │   └── *.txt
│   │       ├── learning/
│   │       │   └── resources.txt
│   │       ├── templates/
│   │       │   └── index.html
│   │       └── static/
│   │           ├── style.css
│   │           └── app.js
│   │
│   └── test/
│       └── java/com/resumeai/resumeanalyzer/
│           └── ResumeAnalyzerUnitTests.java
│
├── .gitignore
├── .gitattributes
├── mvnw
├── mvnw.cmd
├── pom.xml
├── HELP.md
├── ENHANCEMENTS.md
└── README.md
```

### Important source files explained

  -----------------------------------------------------------------------
  File                                Explanation
  ----------------------------------- -----------------------------------
  `ResumeanalyzerApplication.java`    Starts the Spring Boot application.

  `ResumeController.java`             Handles the home page, `/analyze`,
                                      `/upload` and `/coach` requests;
                                      coordinates services and sends data
                                      to Thymeleaf.

  `ResumeParser.java`                 Converts extracted PDF text into
                                      structured resume data and performs
                                      resume checks.

  `SkillExtractor.java`               Separates explicitly listed skills
                                      from skills identified by the local
                                      chat model.

  `ResumeScorer.java`                 Matches role requirements, assigns
                                      evidence statuses and calculates
                                      weighted scores.

  `SemanticMatcher.java`              Compares embeddings to find
                                      related-skill hints.

  `RoadmapGenerator.java`             Builds learning phases and a short
                                      AI-generated summary.

  `ResumeCoach.java`                  Requests safe, evidence-preserving
                                      bullet-point rewrites.

  `RoleKnowledgeBase.java`            Loads role definitions and their
                                      skill aliases.

  `RoadmapKnowledgeBase.java`         Loads role-specific advanced
                                      topics, project ideas and career
                                      steps.

  `LearningKnowledgeBase.java`        Loads learning resources and
                                      supports similarity/exact-key
                                      retrieval.

  `index.html`                        Renders the upload experience and
                                      analysis results.

  `style.css`                         Defines the responsive interface
                                      and themes.

  `app.js`                            Handles theme switching, tabs,
                                      upload feedback, coach submission
                                      and other browser-side
                                      interactions.

  `application.properties`            Configures the application name,
                                      Ollama URL, model names and upload
                                      limits.
  -----------------------------------------------------------------------

## 14. Prerequisites

Install the following before running the project:

-   **Java Development Kit (JDK) 25** --- required by the current
    `pom.xml`.
-   **Git** --- optional for cloning and contributing.
-   **Ollama** --- required for the configured local chat and embedding
    integrations.
-   **VS Code** or another Java IDE --- recommended for development.
-   Internet access on the first Maven dependency resolution, unless
    dependencies are already cached.

Check Java and Git:

``` powershell
java -version
git --version
```

Check Ollama:

``` powershell
ollama --version
```

## 15. Installation and Setup

### Step 1: Clone the repository

``` powershell
git clone https://github.com/sakhib-HubSpace07/AI_resume-analyzer.git
cd AI_resume-analyzer
```

Alternatively, download the repository ZIP from GitHub and extract it.

### Step 2: Install or verify the Ollama models

The application is configured to use:

-   `llama3.2:3b` --- chat model.
-   `nomic-embed-text` --- embedding model.

Pull the models if they are not already present:

``` powershell
ollama pull llama3.2:3b
ollama pull nomic-embed-text
```

Pulling a model that is already installed normally checks its
availability and does not create a second separately named installation.

Verify installed models:

``` powershell
ollama list
```

### Step 3: Start Ollama

Make sure the Ollama service is running. In a separate terminal, you can
start it with:

``` powershell
ollama serve
```

If Ollama is already running in the background, do not start a second
server process.

### Step 4: Open the project

Open the cloned `AI_resume-analyzer` directory in VS Code.

### Step 5: Run tests

``` powershell
.\mvnw.cmd test
```

### Step 6: Start the application

``` powershell
.\mvnw.cmd spring-boot:run
```

## 16. Running the Application

When Spring Boot starts successfully, open:

**http://localhost:8080**

To stop the application, focus the terminal running Spring Boot and
press `Ctrl + C`.

### macOS / Linux commands

Use the Unix Maven wrapper:

``` bash
./mvnw test
./mvnw spring-boot:run
```

## 17. Using the Application

1.  Open `http://localhost:8080`.
2.  Select the career role that you want to target.
3.  Choose a text-readable PDF resume (maximum 5 MB).
4.  Click **Analyze my resume**.
5.  Wait for PDF extraction and local AI operations to finish.
6.  Review the Overview tab for the score and score dimensions.
7.  Open Resume breakdown to inspect parsed resume sections and checks.
8.  Open Skill match to review proven, listed, AI-inferred and missing
    skills.
9.  Open Detected skills to inspect the extracted skill list.
10. Open Roadmap to review learning phases and recommended next steps.
11. Open AI coach to request improvements for up to three bullet points.
12. Review Raw text if you need to inspect what PDFBox extracted.

The local AI model may take longer on the first request or on machines
with limited resources.

## 18. Configuration

The main configuration is in:

`src/main/resources/application.properties`

Current settings:

``` properties
spring.application.name=resumeanalyzer
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2:3b
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
spring.servlet.multipart.file-size-threshold=5MB
spring.ai.ollama.chat.options.temperature=0.2
spring.ai.ollama.embedding.options.model=nomic-embed-text
```

The semantic similarity threshold is read by `SemanticMatcher` from:

``` properties
app.semantic.threshold=0.72
```

If omitted, the Java service uses `0.72` as its default.

Other customization points:

-   **Change the Ollama host:** update `spring.ai.ollama.base-url`.
-   **Change the chat model:** update
    `spring.ai.ollama.chat.options.model`.
-   **Change the embedding model:** update
    `spring.ai.ollama.embedding.options.model`.
-   **Change upload limits:** update the Spring multipart properties and
    keep the controller's explicit 5 MB validation in sync.
-   **Change role requirements:** edit the files in
    `src/main/resources/roles/`.
-   **Change roadmap steps:** edit the files in
    `src/main/resources/roadmaps/`.
-   **Change learning guidance:** edit
    `src/main/resources/learning/resources.txt`.

Restart the application after changing configuration or knowledge files.

## 19. Privacy and Data Handling

The upload flow reads the uploaded PDF bytes and extracts text during
the request. The current controller does not write the uploaded PDF to
an application upload directory or database.

The application is configured to call Ollama at
`http://localhost:11434`, which is intended to be a local service. The
chat and embedding operations send relevant resume text or extracted
phrases to that configured Ollama service.

Keep in mind:

-   The application should be run on a device and network you trust.
-   If you change the Ollama base URL to a remote host, the relevant
    text will be sent to that host.
-   Application/server logs may contain diagnostic information about
    failures; do not enable verbose logging with sensitive resume data
    in production.
-   This repository does not establish a formal privacy certification or
    guarantee that every external environment is private.
-   Do not upload another person's resume without permission.

## 20. Testing

The current test source includes focused unit tests for:

-   Parsing core and bonus skills from a role definition.
-   Extracting contact information and common sections from sample
    resume text.
-   Checking score-level and verdict thresholds.

Run the test suite:

``` powershell
.\mvnw.cmd test
```

A successful Maven test run should end with:

``` text
BUILD SUCCESS
```

The test suite is not a substitute for manually testing the full
browser-to-Ollama workflow. Also test with representative PDF files and
with Ollama unavailable to observe the fallback behavior.

## 21. Current Limitations and Future Improvements

### Current limitations visible in the implementation

-   **Scanned PDFs:** PDFBox extracts embedded text; image-only/scanned
    documents need OCR, which is not currently implemented.
-   **AI dependency:** AI-inferred skills, roadmap summaries and
    coaching depend on the configured Ollama chat model.
-   **Embedding dependency:** related-skill hints and semantic
    learning-resource retrieval depend on the configured embedding
    model; exact-key lookup is available as a fallback for learning
    resources.
-   **Heuristic parsing:** resumes use varied layouts, headings and
    formatting. Rule-based parsing may not interpret every PDF
    perfectly.
-   **Advisory score:** the score reflects this project's role knowledge
    base and formula. It is not an ATS score, hiring prediction or
    guarantee of an interview.
-   **Knowledge coverage:** role skills and learning content are limited
    to the text resources included in this repository.
-   **No user accounts:** the current application does not provide
    registration, login or saved user profiles.

### Potential future improvements

These are ideas for future development, not claims that they are already
implemented:

-   OCR support for scanned PDFs.
-   More robust parsing for multi-column and highly designed resumes.
-   Downloadable analysis reports.
-   More role definitions and configurable skill libraries.
-   Editable roadmap progress saved to a user profile.
-   More extensive controller, integration and end-to-end tests.
-   Accessibility improvements and internationalization.
-   Configurable model selection and health checks for Ollama.
-   Deployment profiles, containerization and production security
    hardening.

## 22. Troubleshooting

  -----------------------------------------------------------------------
  Problem                             What to check
  ----------------------------------- -----------------------------------
  `java` is not recognized            Install JDK 25 and configure
                                      `JAVA_HOME` and `PATH`. Restart the
                                      terminal.

  Maven wrapper cannot download Maven Check internet/proxy access and
                                      Maven Central availability. Try
                                      again after connectivity is
                                      restored.

  Ollama connection refused           Start Ollama and confirm it is
                                      listening at
                                      `http://localhost:11434`.

  Model not found                     Run `ollama list`, then pull the
                                      exact model name configured in
                                      `application.properties`.

  AI results are slow                 Allow more time, check system
                                      memory/CPU and verify that the
                                      selected models can run on your
                                      machine.

  PDF is rejected                     Check that it is a valid PDF, has a
                                      `.pdf` filename and is no larger
                                      than 5 MB.

  Extracted text is empty or garbled  The PDF may be scanned, image-only,
                                      encrypted or use unusual text
                                      encoding. OCR is not currently
                                      included.

  Port 8080 is already in use         Stop the other process using the
                                      port or configure a different
                                      Spring Boot server port.

  Results look incomplete             Inspect the Raw text tab to see
                                      what PDFBox extracted, then check
                                      the relevant role definition and
                                      parser behavior.
  -----------------------------------------------------------------------

## 23. Contributing

Contributions, suggestions, bug reports and improvements are welcome.

1.  Fork the repository.

2.  Create a feature branch:

    ``` bash
    git checkout -b feature/your-feature-name
    ```

3.  Make focused changes and add or update tests.

4.  Run the tests:

    ``` powershell
    .\mvnw.cmd test
    ```

5.  Commit your changes:

    ``` bash
    git add .
    git commit -m "Describe your change"
    ```

6.  Push your branch and open a Pull Request.

Please include a clear description of the problem, the changes made and
how you tested them. Avoid committing API keys, credentials, private
resumes, generated build output or machine-specific configuration.

### Contributors

The repository's current contributor list should be taken from GitHub
rather than guessed or manually attributed. View the live list here:

**[GitHub
Contributors](https://github.com/sakhib-HubSpace07/AI_resume-analyzer/graphs/contributors)**

Thank you to everyone who contributes through code, testing,
documentation, issue reports and suggestions.

## 24. License

No license file was present in the project files reviewed for this
README. Until a license is added to the repository, users should not
assume that the project is available under an open-source license.

If you intend to publish the project for reuse, add a `LICENSE` file and
select a license that matches your intentions.

## 25. Developer and Contact

**Developed by: `sakhib-HubSpace07`**

This project is maintained in the GitHub repository below.

-   **GitHub Profile:** <https://github.com/sakhib-HubSpace07>
-   **Project Repository:** [AI Resume
    Analyzer](https://github.com/sakhib-HubSpace07/AI_resume-analyzer)
-   **LinkedIn Profile:** [Add your LinkedIn profile URL
    here](https://www.linkedin.com/)

> Replace the LinkedIn link above with your personal LinkedIn profile
> URL before publishing this README.

------------------------------------------------------------------------

```{=html}
<p align="center">
```
`<strong>`{=html}AI Resume Analyzer`</strong>`{=html}`<br>`{=html} Built
to turn resume feedback into clear, practical next steps.
```{=html}
</p>
```



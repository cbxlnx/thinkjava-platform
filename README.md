# ThinkJava Platform

An adaptive full-stack learning platform that helps beginner Java students assess their skills, follow a personalised study path, and get lesson-scoped AI tutoring.

## Overview

ThinkJava Platform is designed for learners who need more structure than a static course and more guidance than a generic coding reference. Instead of treating every student the same, the application starts with a diagnostic assessment, estimates topic mastery across core Java checkpoints, and uses that baseline to recommend what to study next.

What makes the project distinctive is the combination of curriculum logic, progress tracking, and a lesson-aware AI tutor. The system does not just present content; it adapts lesson access, recommendations, and dashboard summaries based on diagnostic results and ongoing quiz performance.

## Tech Stack

### Frontend

- Angular 20
- TypeScript
- RxJS
- Angular Router
- `marked` and `ngx-markdown` for lesson content rendering

### Backend

- Java 17
- Spring Boot 3
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Boot Actuator
- JWT authentication via JJWT
- Maven

### Database

- PostgreSQL 16
- `pgvector` for vector similarity search
- SQL migrations and seed scripts

### APIs / External Services

- OpenAI Responses API for tutor answers
- OpenAI Embeddings API for semantic retrieval

### AI / ML Components

- Embedding generation for lesson blocks
- Semantic search over lesson content
- Retrieval-augmented lesson tutor constrained to current lesson context

## Features

- User registration and login with JWT-based authentication.
- Diagnostic assessment across Java fundamentals, loops, arrays, methods, and OOP.
- Personalised starting point selection based on diagnostic performance.
- Mastery tracking per checkpoint, updated from both diagnostic baseline and lesson progress.
- Adaptive lesson catalogue with difficulty locking and recommendation logic.
- Structured lesson pages with markdown content, videos, and quizzes.
- Quiz submission and scoring with pass/fail outcomes and next-lesson recommendation.
- Dashboard summary showing progress, quiz averages, current focus, recent activity, and checkpoint mastery.
- AI tutor that answers questions using retrieved sections from the current lesson rather than broad unrestricted generation.
- Automated testing across frontend unit tests, backend tests, and Playwright user-flow coverage.

## Architecture

The system follows a standard client-server architecture. The Angular frontend handles navigation, authentication state, lesson rendering, and dashboard views. It communicates with the Spring Boot backend over REST APIs, sending JWTs on protected requests. The backend owns business logic for authentication, diagnostics, recommendations, lesson progression, mastery scoring, and AI tutor orchestration.

PostgreSQL stores users, diagnostic results, lesson metadata, lesson blocks, quiz questions, lesson progress, and mastery values. For AI-assisted tutoring, lesson blocks are embedded using OpenAI embeddings and stored in a format that can be queried with `pgvector`. When a learner asks a tutor question, the backend embeds the query, retrieves the most relevant lesson blocks from the current lesson, builds grounded context, and sends that context to the OpenAI Responses API to produce a lesson-bounded answer.

### High-Level Flow

1. User registers or logs in.
2. User completes the diagnostic assessment.
3. Backend stores diagnostic results and initializes mastery per checkpoint.
4. Frontend requests recommended lessons, dashboard data, and lesson details from REST endpoints.
5. Quiz results update lesson progress and recompute mastery.
6. Tutor questions trigger retrieval over lesson block embeddings and model inference with contextual grounding.

## Installation & Setup

### Prerequisites

- Node.js 20+ and npm
- Java 17
- Maven 3.9+
- Docker Desktop or Docker Engine

### 1. Clone the repository

```bash
git clone https://github.com/cbxlnx/thinkjava-platform.git
cd thinkjava-platform
```

### 2. Start the database

From the project root:

```bash
docker compose up -d
```

This starts PostgreSQL with `pgvector` enabled and runs the SQL files in `database/init`.

### 3. Configure environment variables for the backend

Create environment variables in your shell before starting the backend.

PowerShell example:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/thinkjava"
$env:SPRING_DATASOURCE_USERNAME="thinkjava"
$env:SPRING_DATASOURCE_PASSWORD="thinkjava"
$env:JWT_SECRET="your-long-random-secret"
$env:OPENAI_API_KEY="your-openai-api-key"
$env:OPENAI_EMBEDDING_MODEL="text-embedding-3-small"
$env:OPENAI_CHAT_MODEL="gpt-4o-mini"
```

Optional local profile:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
```

### 4. Run the backend

```bash
cd backend
mvn spring-boot:run
```

The backend runs on `http://localhost:8080`.

### 5. Run the frontend

In a new terminal:

```bash
cd frontend
npm install
npm start
```

The frontend runs on `http://localhost:4200`.

## Environment Variables

### Backend

- `PORT`: Backend port. Defaults to `8080`.
- `SPRING_DATASOURCE_URL`: PostgreSQL JDBC connection string.
- `SPRING_DATASOURCE_USERNAME`: Database username.
- `SPRING_DATASOURCE_PASSWORD`: Database password.
- `JWT_SECRET`: Secret used to sign JWT tokens.
- `JWT_EXPIRATION_MILLIS`: Token lifetime in milliseconds. Defaults to `604800000`.
- `OPENAI_API_KEY`: API key used for embeddings and tutor responses.
- `OPENAI_EMBEDDING_MODEL`: Embedding model name. Defaults to `text-embedding-3-small`.
- `OPENAI_CHAT_MODEL`: Chat model name used by the tutor. Defaults to `gpt-4o-mini`.
- `SPRING_PROFILES_ACTIVE`: Optional Spring profile such as `local`.

### Frontend

The frontend currently uses Angular environment files rather than runtime `.env` files:

- `src/environments/environment.ts`: local API base URL
- `src/environments/environment.prod.ts`: production API base URL

## Usage Guide

### Start the App

1. Start PostgreSQL with Docker.
2. Run the backend on port `8080`.
3. Run the frontend on port `4200`.
4. Open `http://localhost:4200`.

### Create an Account or Log In

1. Go to the register page and create an account with email and password.
2. After authentication, the app stores a JWT locally and uses it for protected API calls.
3. Returning users can log in from the login page.

### Use the Main Features

1. Complete the diagnostic assessment to establish your baseline.
2. Review your dashboard summary and current recommended focus.
3. Open the learning area to view available lessons.
4. Work through lesson content and videos, then complete the quiz.
5. Revisit the dashboard to see updated mastery and progress.
6. Ask the AI tutor questions while viewing a lesson for contextual help.

## API Endpoints

### Health

- `GET /api/ping` - Basic API health check.

### Authentication

- `POST /api/auth/register` - Register a new user and return a JWT.
- `POST /api/auth/login` - Authenticate a user and return a JWT.

### User

- `GET /api/users/me` - Return the current authenticated user profile.
- `PATCH /api/users/me/name` - Update the current user's first name.

### Diagnostic

- `GET /api/diagnostic/status` - Check whether the user has completed the diagnostic.
- `POST /api/diagnostic/complete` - Submit and store diagnostic results.
- `GET /api/diagnostic/result` - Retrieve the stored diagnostic result.

### Dashboard

- `GET /api/dashboard/summary` - Return dashboard metrics, current focus, recent activity, and checkpoint mastery.

### Learning

- `GET /api/learn/path` - Return personalised path metadata and mastery map.
- `GET /api/learn/lessons` - Return all lessons with status, progress, and lock state.
- `GET /api/learn/recommendations` - Return recommended lessons based on weak areas and access level.
- `GET /api/learn/current-focus` - Return the user's current best next lesson.
- `GET /api/learn/lesson/{id}` - Return lesson details and quiz data.
- `POST /api/learn/lesson/{id}/quiz/submit` - Submit quiz answers and update progress/mastery.
- `POST /api/learn/debug/recompute-mastery` - Recompute mastery values for the authenticated user.

### Tutor

- `POST /api/tutor/ask` - Retrieve relevant lesson sections and generate a grounded tutor answer.

## AI / ML Components

The AI tutor is implemented as a lightweight retrieval-augmented flow focused on lesson content. Each lesson is broken into blocks, and those blocks can include markdown, videos, and embedding text. When embeddings are generated, they are stored for semantic lookup in PostgreSQL using `pgvector`.

At question time, the backend:

1. Embeds the learner's question with the OpenAI Embeddings API.
2. Searches for the most relevant lesson blocks within the current lesson.
3. Builds a combined context string from the matched lesson blocks.
4. Sends the question, lesson title, and retrieved context to the OpenAI Responses API.
5. Returns both the generated answer and metadata about the matched sections.

This keeps answers grounded in the current lesson and reduces the risk of the tutor drifting into unrelated Java topics.

## Testing

### Frontend

```bash
cd frontend
npm test
```

Runs Angular unit tests with Karma/Jasmine.

### Backend

```bash
cd backend
mvn test
```

Runs backend tests, including smoke, integration, and service-level coverage.

### Current Coverage Areas

- Frontend component/unit tests
- Backend API smoke tests
- Backend authentication integration tests
- Backend learning service unit tests

## Deployment

The frontend production environment is configured to call a backend hosted at `https://thinkjava-platform.onrender.com`, which suggests a cloud deployment path for the API. For local development, PostgreSQL is containerised with Docker Compose and the frontend/backend run separately in development mode.

If you deploy this project yourself, a typical setup would be:

- Frontend: static Angular build served from a hosting platform
- Backend: Spring Boot service deployed to a cloud runtime
- Database: managed PostgreSQL instance with `pgvector` support
- Secrets: injected through environment variables

## Limitations & Future Work

### Known Issues

- Tutor quality depends on embedding freshness and the quality of stored lesson block text.
- The current tutor flow is limited to the active lesson context and does not support cross-lesson reasoning.
- Frontend configuration relies on Angular environment files rather than runtime environment injection.
- Screenshot assets and deployment automation are not yet documented in-repo.

### Planned Improvements

- Add richer admin/content management workflows for lessons and quizzes.
- Expand analytics and learner progress visualisations.
- Improve tutor observability, prompt controls, and answer citations.
- Add CI/CD pipelines for test automation and deployment.
- Document production infrastructure in more detail.

## Contributing

Contributions are welcome. If you plan to contribute:

1. Fork the repository.
2. Create a feature branch.
3. Keep changes focused and well-tested.
4. Run frontend and backend tests before opening a pull request.
5. Document any new environment variables, endpoints, or architecture changes.


## Acknowledgements

- Angular for the frontend application framework.
- Spring Boot and Spring Security for backend APIs and authentication.
- PostgreSQL and `pgvector` for persistence and semantic retrieval.
- OpenAI for embeddings and tutor response generation.
- Playwright, Karma, and JUnit for testing support.
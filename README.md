# Job Application Intelligence

> 🚧 **Active Development** — This project is currently being built and is **not yet a finished product**.

A full-stack job application management platform designed to help candidates organize their job search, manage applications, maintain career profiles, and eventually use their application data to make more informed decisions throughout the job-search process.

This repository is being developed incrementally with a focus on **production-quality engineering practices**, including authentication and security, database migrations, automated testing, API design, documentation, and a structured Git workflow.

---

## Project Status

The project is currently in the **backend foundation phase**.

The backend has been scaffolded with Spring Boot and PostgreSQL, and the first major feature — **session-based authentication** — has been implemented and tested.

### Currently implemented

* Spring Boot backend
* PostgreSQL database
* Flyway database migrations
* User persistence
* User registration
* User login
* Current-user endpoint
* Logout
* BCrypt password hashing
* Server-side HTTP sessions
* CSRF protection
* Credentialed CORS configuration
* Request validation
* Structured API error responses
* PostgreSQL integration testing with Testcontainers
* Authentication/security documentation

### Current milestone

**Authentication foundation → Complete**

The authentication implementation was developed through a feature branch and pull request, reviewed, tested, and merged into the integration branch.

The current backend authentication test suite contains **23 tests with 0 failures, 0 errors, and 0 skipped tests**.

> The frontend has not been implemented yet. Application management, resumes, profiles, analytics, and other product features are planned but are not currently available.

---

## Vision

The long-term goal is to build a single application that can act as a candidate's **career/job-search workspace**.

The planned product will allow users to:

* Maintain a reusable professional profile
* Store and manage resumes
* Discover and save job opportunities
* Track applications throughout their lifecycle
* Record interviews, offers, rejections, and other application events
* Analyze application history
* Eventually use accumulated application data to provide useful insights

The project is intentionally being built in stages rather than trying to implement the entire product at once.

---

## Planned Features

The following represents the planned direction of the project, not the current feature set.

### 1. Authentication & Accounts

* [x] User registration
* [x] User login
* [x] Session management
* [x] Current-user endpoint
* [x] Logout
* [x] Password hashing
* [x] CSRF protection
* [x] Authentication integration tests

### 2. Candidate Profile

* [ ] Personal information
* [ ] Education
* [ ] Work experience
* [ ] Skills
* [ ] Projects
* [ ] Certifications
* [ ] Profile management API

### 3. Resume Management

* [ ] Resume upload
* [ ] Resume storage
* [ ] Resume parsing
* [ ] Resume-to-profile autofill
* [ ] Multiple resume versions

### 4. Job Management

* [ ] Job creation/manual entry
* [ ] Job board integration
* [ ] Job search
* [ ] Save jobs
* [ ] Job details and requirements
* [ ] Match jobs against candidate profile

### 5. Application Tracking

* [ ] Apply/save application
* [ ] Application status tracking
* [ ] Application timeline
* [ ] Interview tracking
* [ ] Offer tracking
* [ ] Rejection tracking

### 6. Analytics & Intelligence

* [ ] Application statistics
* [ ] Response-rate analysis
* [ ] Interview-rate analysis
* [ ] Application funnel
* [ ] Job/skill trend analysis
* [ ] Data-driven application insights

### 7. Frontend

* [ ] React client
* [ ] Authentication flows
* [ ] Candidate dashboard
* [ ] Profile management
* [ ] Job board
* [ ] Application tracker
* [ ] Analytics dashboard

---

## Current Architecture

The project is being developed as a full-stack application with a separate frontend and backend.

### Backend

* **Java 21**
* **Spring Boot 4.1.1**
* **Spring Security**
* **Spring Data JPA**
* **PostgreSQL**
* **Flyway**
* **Maven**
* **Testcontainers**
* **JUnit / Spring testing**

The backend currently follows a layered approach around controllers, services, persistence, DTOs, and security configuration.

### Authentication

Authentication currently uses **server-side HTTP sessions rather than JWT**.

The basic flow is:

```text
Registration
    ↓
Request validation
    ↓
BCrypt password hashing
    ↓
PostgreSQL persistence
    ↓
Authentication
    ↓
Server-side HTTP session
    ↓
JSESSIONID cookie
```

Authenticated requests use the session to restore the user's security context.

CSRF protection is enabled for state-changing requests, and credentialed CORS is explicitly configured for the development frontend origin.

More detailed authentication and security decisions are documented in:

* [Authentication API & Security](docs/authentication.md)

---

## API

The current API is focused on authentication.

| Method | Endpoint             | Purpose                         |
| ------ | -------------------- | ------------------------------- |
| `POST` | `/api/auth/register` | Register a new user             |
| `POST` | `/api/auth/login`    | Authenticate a user             |
| `GET`  | `/api/auth/me`       | Retrieve the authenticated user |
| `POST` | `/api/auth/logout`   | End the current session         |
| `GET`  | `/api/auth/csrf`     | Retrieve a CSRF token           |

More detailed request/response contracts and security behavior are documented in the authentication documentation.

---

## Testing

Testing is being treated as part of the feature implementation rather than something added after the application is built.

The current authentication implementation includes:

* Unit tests
* Repository tests
* Spring Security tests
* Integration tests
* PostgreSQL integration tests using Testcontainers
* CSRF verification
* CORS verification
* Session-cookie verification
* Authentication failure scenarios
* Logout/session invalidation tests

Run the backend test suite from the `backend` directory:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

Docker is required for the Testcontainers-based PostgreSQL tests.

---

## Development Workflow

The project is being developed using a structured Git workflow rather than committing all work directly to the production branch.

```text
main
  ↑
  │
dev
  ↑
  │
feature branches
```

### Branches

* `main` — stable/production-ready code
* `dev` — integration branch for completed features
* `feature/*` — individual features or pieces of work

Features are developed through:

1. GitHub Issue
2. Feature branch
3. Implementation
4. Tests
5. Documentation where appropriate
6. Pull request
7. Review
8. Merge into `dev`
9. Promotion of `dev` into `main`

The intention is to keep the repository history representative of how a real software project is developed and maintained.

---

## Project Documentation

Technical decisions and feature-specific documentation live alongside the code.

Current documentation:

* [Authentication API & Security](docs/authentication.md)

As the project grows, additional documentation will be added for areas such as:

* Database design
* API architecture
* Resume processing
* Application domain model
* Frontend architecture
* Deployment
* Security decisions

---

## Repository Structure

The repository is currently organized around the backend and project documentation:

```text
job-application-intelligence/
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
│
├── docs/
│   └── authentication.md
│
├── run-dev.ps1
├── .gitignore
└── README.md
```

The frontend will be introduced as the project progresses.

---

## Development Philosophy

This project is being built as a long-running engineering project rather than a one-off demo.

The emphasis is on:

* Understanding architectural decisions
* Building features incrementally
* Writing tests alongside features
* Maintaining clear API contracts
* Treating security as a first-class concern
* Using database migrations
* Keeping technical decisions documented
* Reviewing changes through pull requests
* Maintaining a clean Git history
* Expanding the system only after the underlying foundation is sound

The current implementation is intentionally small. The goal is to build the system properly before adding the larger product features on top of it.

---

## Current Focus

> **Current focus: building the backend foundation and establishing the core application architecture.**

The next stages will build on the authentication foundation and gradually introduce the candidate profile, resume, job, and application domains.

This README will be updated as the project progresses.

---

## Status

**🚧 Actively being developed**

This repository represents an ongoing project. Features, architecture, APIs, and documentation will continue to evolve as development progresses.

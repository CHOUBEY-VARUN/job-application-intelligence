# Authentication API and security

Issue #7 establishes the backend authentication foundation consumed by the future React client. The current implementation uses Spring Security with server-side HTTP sessions; it does not use JWT.

## Authentication model

Registration validates the request, BCrypt-hashes the password, persists the user in the existing `users` table, and authenticates the newly registered account. Login sends the submitted credentials to Spring Security's `AuthenticationManager`, which uses the configured `UserDetailsService` to load the account and the password encoder to verify its hash. On success, the authenticated `SecurityContext` is saved in the server-side HTTP session.

The browser receives the `JSESSIONID` cookie, which identifies that server-side session. On a later request, the session restores the `SecurityContext` and Spring Security makes the authenticated identity available to the API. Registration and login responses contain the user's ID and email; they do not contain a password or password hash.

Conceptually:

```text
Registration: request → validation → BCrypt hashing → persistence
Login: credentials → AuthenticationManager → UserDetailsService → password verification
        → SecurityContext → server-side session → JSESSIONID
Authenticated request: JSESSIONID → server-side session → SecurityContext → user identity
```

## API contract

All endpoints are under `/api/auth`. JSON request bodies use `Content-Type: application/json`. POST requests require a valid CSRF token, including registration and login, even though those endpoints are public with respect to authentication.

| Method and path | Authentication | Request | Success | Important errors | CSRF / session behavior |
| --- | --- | --- | --- | --- | --- |
| `POST /api/auth/register` | Not required | `{"email":"person@example.com","password":"at-least-8-chars"}`. Email must be nonblank and valid; password must be 8–64 characters. | `201 Created`, `{"id":"<UUID>","email":"person@example.com"}` | `400` validation error; `409` duplicate email (`EMAIL_ALREADY_REGISTERED`); `403` missing/invalid CSRF token | Requires CSRF. Creates a user, authenticates it, and creates/saves a server-side session identified by `JSESSIONID`. |
| `POST /api/auth/login` | Not required | `{"email":"person@example.com","password":"at-least-8-chars"}`. The request DTO applies the same email and password validation. | `200 OK`, `{"id":"<UUID>","email":"person@example.com"}` | `400` validation error; `401` invalid credentials (`INVALID_CREDENTIALS`); `403` missing/invalid CSRF token | Requires CSRF. Successful authentication saves the `SecurityContext` in a server-side session and returns `JSESSIONID`. |
| `GET /api/auth/me` | Required | No body | `200 OK`, `{"id":"<UUID>","email":"person@example.com"}` | `401` if unauthenticated | No CSRF token required for this safe GET. Uses the session identified by `JSESSIONID`; does not create a new authentication. |
| `POST /api/auth/logout` | No authentication matcher is configured for this Spring Security logout-filter route; calling it ends the current session when one exists. | No body | `204 No Content` | `403` if the CSRF token is missing/invalid | Requires CSRF. Invalidates the HTTP session and clears authentication. |
| `GET /api/auth/csrf` | Not required | No body | `200 OK`, a Spring `CsrfToken` JSON object containing `token`, `headerName`, and `parameterName` | No application-specific error response is defined for this endpoint | No CSRF token required to fetch it. The CSRF repository sets the `XSRF-TOKEN` cookie; this endpoint exposes the token and header name for the client. It does not authenticate or create a login session. |

Validation errors use the application's `ErrorResponse` shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "errors": {
    "email": "must be a well-formed email address"
  }
}
```

The `errors` object maps invalid field names to the first validation message for each field. Other application errors use `code` and `message`; the `errors` property is null where no field errors apply. Invalid login credentials intentionally share the same response whether the email is unknown or the password is wrong. CSRF rejections are handled by Spring Security and are not mapped to the application's `ErrorResponse` format.

## Browser request flow

The client first calls `GET /api/auth/csrf`, reads the returned `token` and `headerName`, and sends that token in the named header (currently `X-XSRF-TOKEN`) on state-changing requests. The browser must send credentials so it accepts and returns the session cookie. For the Fetch API, use `credentials: "include"`; for Axios, use `withCredentials: true`.

After registration or login, call `GET /api/auth/me` with credentials to determine the current identity. Call `POST /api/auth/logout` with credentials and a fresh/current CSRF token to invalidate the session.

CSRF protection remains enabled because browsers automatically attach session cookies to requests. CORS controls which browser origins can read credentialed responses; it does not authenticate users or replace CSRF protection.

## Security decisions

### Passwords

Passwords are BCrypt-hashed before persistence. The stored value is kept in `password_hash`; API responses expose neither the submitted password nor its hash.

### Sessions and cookies

Authentication state is stored server-side. The `JSESSIONID` cookie identifies that session and is configured with:

| Setting | Value | Purpose |
| --- | --- | --- |
| Name | `JSESSIONID` | Explicit session-cookie name used by the API and tests. |
| Path | `/` | Sends the cookie to application routes. |
| HttpOnly | `true` | Prevents JavaScript from reading the session identifier. |
| SameSite | `Lax` | Restricts cross-site cookie sending while supporting the local frontend/backend setup. |
| Secure | `false` by default; controlled by `SESSION_COOKIE_SECURE` | Keeps local HTTP development usable. Set to `true` when the backend is served over HTTPS. |

The session cookie stays HttpOnly; the separate `XSRF-TOKEN` cookie is readable by the frontend so the client can copy its token into the request header. Production deployments must serve the API over HTTPS and set `SESSION_COOKIE_SECURE=true`. If a future deployment places the frontend and API on different sites, revisit SameSite and CSRF settings together; `SameSite=None` also requires Secure cookies.

### CSRF

Spring Security CSRF protection is enabled for state-changing methods. `CookieCsrfTokenRepository` writes `XSRF-TOKEN` and expects the token in `X-XSRF-TOKEN`. The public `GET /api/auth/csrf` endpoint returns the token and header name so a client can initialize this flow. The CSRF cookie uses `HttpOnly=false` for this reason; this does not apply to `JSESSIONID`.

### CORS

CORS is configured for `/api/**` with credentials enabled and one exact allowed origin. The default development origin is `http://localhost:5173`; set `APP_CORS_ALLOWED_ORIGIN` to the actual React development origin if it differs. Allowed methods are `GET`, `POST`, and `OPTIONS`; allowed request headers are `Content-Type` and `X-XSRF-TOKEN`. Valid preflight requests are handled before authentication and CSRF checks.

Credentialed CORS cannot safely use `Access-Control-Allow-Origin: *`, so the configured origin is explicit. No production frontend domain is assumed or included. Set the origin to the deployed frontend's exact origin when such a deployment exists.

## Route access policy

The security configuration explicitly permits `POST /api/auth/register`, `POST /api/auth/login`, and `GET /api/auth/csrf`. The logout filter handles `POST /api/auth/logout` and invalidates the current session when present; CSRF still applies. All other requests require authentication. At this stage there are no profile, resume, skill, or application resource endpoints; ownership checks for those resources must be enforced when those endpoints are introduced.

## Configuration

| Property / environment variable | Default | Use |
| --- | --- | --- |
| `app.cors.allowed-origin` / `APP_CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | Exact browser origin permitted for credentialed API requests. Configure it to the frontend origin for each environment. |
| `server.servlet.session.cookie.secure` / `SESSION_COOKIE_SECURE` | `false` | Set `true` when serving over HTTPS; leave false for local HTTP. |

Example local development values:

```text
APP_CORS_ALLOWED_ORIGIN=http://localhost:5173
SESSION_COOKIE_SECURE=false
```

For HTTPS production deployment, set `SESSION_COOKIE_SECURE=true` and configure `APP_CORS_ALLOWED_ORIGIN` to the real frontend origin. No production domain is defined by this repository today.

## Automated verification

The authentication integration tests run with PostgreSQL Testcontainers and Flyway. They cover registration, duplicate and invalid registration, password encoding, login failures, protected access, CSRF, CORS, the HTTP session-cookie round trip, and logout invalidation. Run the full backend suite from `backend` with `./mvnw test` (or `mvnw.cmd test` on Windows). Docker must be available for Testcontainers.

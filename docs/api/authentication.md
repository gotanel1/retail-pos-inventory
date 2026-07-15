# Authentication API

The browser authenticates with an `HttpOnly` session cookie. State-changing requests must also send a CSRF token. The API never returns an access token for storage in browser JavaScript.

## Login sequence

1. `GET /api/v1/auth/csrf` and retain the returned header name and token.
2. `POST /api/v1/auth/login` with that CSRF header and JSON credentials.
3. Allow the browser to store the session cookie by using `credentials: include`.
4. Call `GET /api/v1/auth/me` to restore the authenticated user after a reload.
5. Fetch a fresh CSRF token after successful login or logout because Spring Security rotates it.

## Example

```http
GET /api/v1/auth/csrf HTTP/1.1
Accept: application/json
```

```json
{
  "headerName": "X-CSRF-TOKEN",
  "token": "generated-token"
}
```

```http
POST /api/v1/auth/login HTTP/1.1
Content-Type: application/json
X-CSRF-TOKEN: generated-token

{
  "username": "owner",
  "password": "configured-secret"
}
```

Authentication and authorization failures use `application/problem+json`. The session cookie is `HttpOnly`, `SameSite=Lax`, and must be marked `Secure` in HTTPS deployments through `SESSION_COOKIE_SECURE=true`.

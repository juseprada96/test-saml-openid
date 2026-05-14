# Sample OpenID Connect Server

A minimal Spring Boot application that acts as an OpenID Connect token issuer. The OIDC discovery
and JWKS endpoints are **hosted statically on InfinityFree** (`http://test-openid-bug.ct.ws`),
while this app runs **locally** to generate signed JWTs.

| Endpoint | Hosted at | Description |
|---|---|---|
| `GET /.well-known/openid-configuration` | InfinityFree (static) | OpenID Connect discovery document |
| `GET /jwks` | InfinityFree (static) | JSON Web Key Set (RSA public key) |
| `GET /token/generate` | localhost | Generate a JWT with default claims |
| `POST /token/generate` | localhost | Generate a JWT with custom user claims |

Tokens are signed with **RS256** using a **static RSA-2048 key pair** stored in
`src/main/resources/keys/`. The same public key is published in the InfinityFree JWKS.

---

## Running locally

```bash
# Build
mvn clean package -DskipTests -s settings-central.xml

# Run
java -jar target/sample-openid-0.0.1-SNAPSHOT.jar
```

The server starts on port **8080** (HTTP only).

---

## Authentication

The `/token/generate` endpoint is protected with **HTTP Basic Auth**.
The OIDC discovery (`/.well-known/openid-configuration`) and JWKS (`/jwks`) endpoints are public.

Default credentials (override with env vars):

| Env var | Default |
|---|---|
| `TOKEN_USERNAME` | `admin` |
| `TOKEN_PASSWORD` | `changeme` |

---

## Token generation

### Default user (GET)
```bash
curl -u admin:changeme http://localhost:8080/token/generate
```

### Custom user (POST)
All fields are optional — omitted fields fall back to defaults.

```bash
curl -u admin:changeme -X POST http://localhost:8080/token/generate \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "givenName": "John",
    "familyName": "Doe",
    "userId": "abc-123",
    "sub": "custom-sub-id",
    "buId": "some-bu-id",
    "audience": "my-client-id",
    "clientId": "my-client-id",
    "groups": ["Admin", "Developer"]
  }'
```

Tokens always have **1 hour** validity (`iat` = now, `exp` = now + 3600s).

---

## InfinityFree static hosting

The `infinityfree/` directory contains files to upload to InfinityFree
(`http://test-openid-bug.ct.ws`):

```
infinityfree/
  .htaccess                              # URL rewrites (removes .php extension)
  jwks.php                               # → /jwks
  .well-known/
    openid-configuration.php             # → /.well-known/openid-configuration
```

Upload all files (preserving the `.well-known/` subdirectory) to the InfinityFree `htdocs/` folder.

> **Important:** The RSA key pair in `src/main/resources/keys/` is static. Do **not** regenerate
> these keys unless you also update `infinityfree/jwks.php` with the new public key values,
> as any previously issued tokens will fail verification.

---

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `ISSUER_URL` | `http://test-openid-bug.ct.ws` | Base URL used in `iss` claim and discovery doc |
| `TOKEN_USERNAME` | `admin` | Basic Auth username for `/token/generate` |
| `TOKEN_PASSWORD` | `changeme` | Basic Auth password for `/token/generate` |

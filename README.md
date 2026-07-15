# Retail POS & Inventory

Portfolio-grade retail point-of-sale and inventory system for a single-store general-goods retailer.

The project demonstrates production-oriented Java backend skills through immutable stock movements, transactional checkout, role-based access control, inventory costing, payment integration, automated tests, and containerized deployment.

## Planned stack

- Java 21, Spring Boot 4.1, Maven
- PostgreSQL 18 and Flyway
- React 19, TypeScript, Vite, Material UI
- Docker Compose, Testcontainers, Playwright
- Stripe PromptPay test-mode integration

## Delivery workflow

Development follows GitHub Flow. Every feature is linked to a GitHub Issue, implemented on a dedicated branch, verified by automated tests, reviewed in a Pull Request, and merged without removing the individual commits.

Commit subjects use Conventional Commits with Thai descriptions. Durable Thai learning notes are kept in `docs/learning`, while architecture decisions are recorded in `docs/adr`.

## Local development

Copy `.env.example` to `.env`, change the local password, then start the complete application:

```powershell
docker compose up --build
```

`APP_BOOTSTRAP_OWNER_PASSWORD` is required only when the database has no users. The application creates the first `OWNER` account, stores only its BCrypt hash, and skips bootstrap on later starts.

PromptPay requires Stripe Test Mode credentials. Set `STRIPE_SECRET_KEY=sk_test_...` and forward local webhooks with Stripe CLI:

```powershell
stripe listen --forward-to localhost:8080/api/v1/payments/stripe/webhook
```

Set `STRIPE_WEBHOOK_SECRET` to the `whsec_...` value printed by that command. Stripe Dashboard webhook secrets and Stripe CLI webhook secrets are different; use the secret from the endpoint that sends the event.

Useful endpoints:

- Application: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- OpenAPI: `http://localhost:8080/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui`

For frontend-only development, run `npm install` and `npm run dev` in `frontend`. Vite proxies `/api` and `/actuator` to Spring Boot on port 8080.

## Verification

```powershell
cd backend
.\mvnw.cmd verify

cd ..\frontend
npm run lint
npm test
npm run build
```

Backend integration tests use Testcontainers and therefore require Docker Desktop to be running.

## Product status

Foundation, RBAC, catalog/CSV, immutable stock ledger, goods receipts, approved stock counts, customer anonymization, cash POS, and Stripe PromptPay Test Mode are implemented. Checkout includes idempotency, pessimistic stock locking, ten-minute PromptPay reservations, signed and deduplicated webhooks, inclusive-VAT snapshots, manager-approved discounts, and browser-print receipts. See [Project Charter](docs/product/project-charter.md) and the API guides in `docs/api`.

## License

No open-source license has been granted. The repository is public for portfolio review; all rights are reserved.

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
npm test
npm run build
```

Backend integration tests use Testcontainers and therefore require Docker Desktop to be running.

## Product status

Foundation development is in progress. See [Project Charter](docs/product/project-charter.md) for the product boundaries and acceptance criteria.

## License

No open-source license has been granted. The repository is public for portfolio review; all rights are reserved.

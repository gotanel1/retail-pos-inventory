# Render Deployment

The root `render.yaml` defines a same-origin Docker web service and a private PostgreSQL 18 database in the Singapore region. It is intended for a portfolio demo containing synthetic data only.

## Prerequisites

- A GitHub repository with the CI workflow passing on `main`
- A Render account connected to that repository
- Stripe Test Mode credentials for the PromptPay demo
- A separate, random demo password of at least 12 characters

## Create the Blueprint

1. In Render, choose **New > Blueprint** and connect this repository.
2. Confirm that Render detects the root `render.yaml`.
3. Enter the prompted secret values:
   - `APP_DEMO_PASSWORD`: shared by `demo-manager`, `demo-cashier`, and `demo-stock`
   - `STRIPE_SECRET_KEY`: a Stripe Test Mode `sk_test_...` key
   - `STRIPE_WEBHOOK_SECRET`: the `whsec_...` secret for the Render webhook endpoint
4. Create the Blueprint and wait for `/actuator/health` to become healthy.
5. In Stripe Test Mode, register `https://<service>.onrender.com/api/v1/payments/stripe/webhook` and subscribe to PaymentIntent events.
6. Replace `STRIPE_WEBHOOK_SECRET` in the Render dashboard if the endpoint secret was created after the first Blueprint sync, then redeploy.

Render supplies a `postgresql://user:password@host/database` private connection string, while pgJDBC expects `jdbc:postgresql://host/database` and separate credentials. A Spring `EnvironmentPostProcessor` converts the URL before DataSource auto-configuration and removes user information from the JDBC URL. Docker Compose continues to override it with `SPRING_DATASOURCE_URL` locally. Database username and password are referenced separately and are not committed.

## Demo accounts

The initializer creates these users only when `APP_DEMO_DATA=true`:

| Username | Role | Intended demo |
| --- | --- | --- |
| `demo-manager` | `MANAGER` | Dashboard, catalog, receipts, stock approval |
| `demo-cashier` | `CASHIER` | Cash and PromptPay checkout |
| `demo-stock` | `INVENTORY_STAFF` | Stock counting |

All accounts use `APP_DEMO_PASSWORD`. Do not reuse this password for an owner account or any other system. There is no public signup.

## Operations and limitations

- `SESSION_COOKIE_SECURE=true` requires HTTPS and keeps the session cookie out of plain HTTP traffic.
- API docs are disabled in the public demo with `SPRINGDOC_ENABLED=false`.
- `APP_PAYMENT_FAKE` must remain `false`; the fake provider is restricted to the local `e2e` Spring profile.
- Flyway validates and applies schema migrations when the application starts.
- The free service and database plans are suitable for a portfolio preview, not a production retailer. Expect cold starts, limited resources, and free-database lifecycle constraints.
- Render deploys from the linked default branch only after its GitHub checks pass because `autoDeployTrigger` is `checksPass`.

For a real pilot, use paid infrastructure, configure backups and monitoring, rotate all credentials, restrict operational access, complete legal/privacy review, and perform a restore drill before accepting real customer data.

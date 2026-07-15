# ADR 0004: Use an Immutable Stock Ledger with Materialized Balances

- Status: Accepted
- Date: 2026-07-15

## Context

The store needs fast stock availability checks while retaining enough evidence to explain every quantity change. Recomputing current stock from all historical movements on every checkout becomes increasingly expensive, but storing only a mutable quantity cannot explain discrepancies or identify the responsible transaction.

## Decision

Maintain two related models inside one PostgreSQL transaction:

- `stock_movements` is an append-only audit ledger. Every movement stores its quantity delta, resulting quantities, cost snapshot, business reference, actor, and timestamp.
- `inventory_balances` stores the current `on_hand`, `reserved`, `available`, and moving-average cost for efficient reads and concurrency control.

PostgreSQL triggers reject updates and deletes against stock movements and posted goods receipts. Corrections must be represented by explicit compensating movements such as `ADJUSTMENT_IN` or `ADJUSTMENT_OUT`.

Commands lock balance rows with `SELECT FOR UPDATE`. When a command touches multiple products, it locks them in deterministic product-ID order to reduce deadlock risk.

## Consequences

- Checkout can validate available stock without replaying the ledger.
- Operators can trace each balance change to a document and actor.
- Balance and ledger must always be updated in the same transaction.
- Direct historical edits are intentionally unavailable; correction workflows require an additional movement.
- Database triggers provide a final integrity boundary beyond application permissions.

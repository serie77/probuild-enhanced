# ProBuild Operations Platform — Architecture

> **Design stance.** ProBuild's operation is modelled as a **process orchestration**, not a
> lane-based choreography. A lean *Customer Journey* orchestrator owns the end-to-end customer
> experience and delegates each unit of back-office work to an **independent, separately
> deployable capability process** invoked through a **BPMN call activity**. External organisations
> (FinTrust, FixPro) are integrated as **autonomous partner processes** reached only through
> **correlated messages**. Business rules live in **DMN decision tables**, not in Java `if`
> statements. Cross-cutting concerns — cancellation, rollback, escalation — are handled with
> **event sub-processes** and **compensation**.
>
> This is a deliberate departure from a single monolithic pool with internal lanes: each capability
> is independently testable, independently deployable, and reusable across journeys (Warehouse, for
> example, is called by both the Buy journey and the Hire-return journey).

The design applies established patterns: BPMN 2.0 collaboration, call activities and message events
(OMG, 2013; Freund and Rücker, 2019); externalised business rules via DMN (OMG, 2021); an
orchestration style with compensation/SAGA for long-running work (Richardson, 2018); and correlated
messaging between autonomous participants (Hohpe and Woolf, 2003), realised on Camunda 8
(Camunda, 2024). Full reference list: `docs/references.md`.

## 1. Process inventory

| Process ID | Kind | Trigger | Responsibility |
|---|---|---|---|
| `Journey_Customer` | Orchestrator (pool: Customer) | Start form | Owns the customer conversation; routes by service type; invokes capabilities; talks to partners by message |
| `Cap_Sales` | Capability (called) | Call activity | Inventory check, DMN pricing, quote, backorder decision |
| `Cap_Warehouse` | Capability (called) | Call activity | Order creation, pick/QC/pack/dispatch (operative tasks); tool-return inspection; triggers FixPro |
| `Cap_Loyalty` | Capability (called) | Call activity | Trade-card issue/renew, points accrual, DMN discount banding |
| `Cap_Hire` | Capability (called) | Call activity | Availability, DMN tiered pricing, booking, tool issue |
| `Partner_FinTrust` | Partner (pool) | Message `finance.requested` | DMN credit assessment + human confirmation; funds transfer or rejection |
| `Partner_FixPro` | Partner (pool) | Message `service.requested` **or** weekly timer | Batch collection, DMN triage, service, PAT test, invoice |

## 2. Decision tables (DMN)

| Decision ID | Inputs | Outputs | Replaces (v1 Java) |
|---|---|---|---|
| `loyalty_band` | `pointsBalance`, `hasCard` | `discountBand`, `discountPct` | `LoyaltyWorkers.updateDiscountBand` if/else |
| `finance_assessment` | `financeAmount`, `annualIncome`, `creditScore` | `financeRecommendation`, `termMonths` | `FinTrustWorkers` term ternary + manual gate |
| `hire_pricing` | `rentalDays` | `tierMultiplier`, `tierLabel` | `PricingPolicy.rentalDiscount` if-ladder |
| `maintenance_triage` | `toolCount` | `routinePct`, `repairPct`, `outOfServicePct` | `FixProWorkers.inspectBatch` constants |

## 3. Call-activity contracts

Each capability declares an explicit input/output contract (Zeebe `ioMapping`), so the orchestrator
and capabilities are decoupled — a capability can be re-tested in isolation by supplying its inputs.

- **Cap_Sales** — in `{customerName, customerEmail, productLines, tradeCardNumber}` → out `{quoteReference, quotedPrice, stockStatus, salesOutcome}` (`QUOTED` | `BACKORDER`).
- **Cap_Warehouse (fulfil)** — in `{quoteReference, productLines, finalAmount, deliveryMethod}` → out `{orderReference, trackingNumber, fulfilOutcome}`.
- **Cap_Warehouse (return)** — in `{bookingReference, toolLines}` → out `{returnOutcome}` (`RESTOCKED` | `SERVICED`).
- **Cap_Loyalty (enrol)** — in `{customerEmail, isRenewal, tradeCardNumber}` → out `{tradeCardNumber, discountBand}`.
- **Cap_Loyalty (accrue)** — in `{tradeCardNumber, quotedPrice}` → out `{pointsBalance, discountBand, discountPct}`.
- **Cap_Hire** — in `{customerName, toolLines}` → out `{bookingReference, rentalFee, hireOutcome}` (`BOOKED` | `UNAVAILABLE`).

## 4. Partner message channels

| Message | From → To | Correlated on |
|---|---|---|
| `finance.requested` | Journey → FinTrust | `journeyId` |
| `finance.settled` | FinTrust → Journey | `journeyId` |
| `service.requested` | Warehouse → FixPro | `journeyId` |
| `service.completed` | FixPro → Warehouse | `journeyId` |

## 5. Cross-cutting patterns

- **Cancellation** — an interrupting **event sub-process** in the orchestrator listens for a cancel
  signal (quote rejected, finance declined) and drives a clean *Customer cancelled* end, replacing
  v1's dead-end where the sales instance hung at a catch event (v1 DEF-03).
- **Compensation (SAGA)** — when a financed order is later declined, a **compensation handler**
  releases reserved stock and voids the draft order, so no partial state is left behind.
- **Error boundaries** — risky service tasks (inventory, finance settlement) carry BPMN **error
  boundary events** with a retry/incident path rather than silently auto-completing.

## 6. Package layout (fresh)

```
com.probuild.platform
├── PlatformApplication.java          # Spring Boot entry + @Deployment
├── shared/    Journey, Messages, Decisions, Money  (cross-cutting helpers)
├── data/      entity/  repo/  Seeder
├── sales/     SalesHandlers
├── warehouse/ WarehouseHandlers
├── loyalty/   LoyaltyHandlers
├── hire/      HireHandlers
├── fintrust/  FinTrustHandlers
└── fixpro/    FixProHandlers
```

Each capability package owns exactly the job-worker handlers for its process and injects only the
repositories it needs. `shared/` holds the message-publication choke point, the DMN evaluation
wrapper, and money/variable utilities.

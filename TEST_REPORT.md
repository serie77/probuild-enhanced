# ProBuild Operations Platform (v2) — Test Execution Report

## Document control

| Field | Value |
|---|---|
| System under test | ProBuild Operations Platform — orchestration architecture (Camunda 8) |
| Processes | `Journey_Customer` (3-pool collaboration), `Cap_Sales`, `Cap_Warehouse`, `Cap_Loyalty`, `Cap_Hire`, `Partner_FinTrust`, `Partner_FixPro` |
| Decision tables | `finance_assessment`, `loyalty_band`, `hire_pricing`, `maintenance_triage` |
| Workers | 24 `@JobWorker` handlers across seven capability packages |
| Engine | Camunda 8.8.11 Self-Managed (c8run) · Java 21 · Spring Boot 3.5.9 |
| Tester | MA |
| Execution date | 13 July 2026 |

## Approach

Each journey path is driven **through the live engine's REST API** end-to-end: an instance of
`Journey_Customer` is started, user tasks are completed in sequence (across the Customer, FinTrust
and FixPro pools), automatic service tasks and DMN business-rule tasks execute, and the outcome is
asserted three ways — the **process reaches the expected end event**, the **called capabilities
and partner instances complete**, and the **H2 database holds the correct rows**. The driver is
`suite.mjs`; raw results are in `docs/evidence/journey-results.json`.

## Pre-flight

| # | Check | Result |
|---|---|---|
| 1 | App starts; log shows all 7 processes + 4 DMN deployed | PASS |
| 2 | 24 job workers registered | PASS |
| 3 | Operate shows all processes at version, active | PASS |
| 4 | Seed: 3 trade cards, 11 inventory lines | PASS |

## Execution summary

| ID | Scenario | Path exercised | End event | Result |
|---|---|---|---|---|
| TC-CARD-01 | Apply Trade Card (new) | orchestrator → `Cap_Loyalty` (issue + `loyalty_band` DMN) | Customer served | PASS |
| TC-CARD-02 | Renew Trade Card | orchestrator → `Cap_Loyalty` (renew + DMN) | Customer served | PASS |
| TC-BUY-01 | Buy in stock, card payment | `Cap_Sales` → `Cap_Warehouse` → `Cap_Loyalty` | Customer served | PASS |
| TC-BUY-02 | Buy with finance **approved** | + FinTrust pool, `finance_assessment` DMN → APPROVE | Customer served | PASS |
| TC-BUY-03 | Buy with finance **declined** | FinTrust `finance_assessment` DMN → DECLINE (low score) | Customer cancelled | PASS |
| TC-BUY-04 | Buy out of stock | `Cap_Sales` → backorder | Customer cancelled | PASS |
| TC-BUY-05 | Buy, quote rejected | `Cap_Sales` → quote → reject | Customer cancelled | PASS |
| TC-HIRE-01 | Hire + **maintenance** | `Cap_Hire` (`hire_pricing` DMN) → `Cap_Warehouse` → FixPro (`maintenance_triage` DMN) | Customer served | PASS |
| TC-HIRE-02 | Hire, clean return | `Cap_Hire` → `Cap_Warehouse` (restock) | Customer served | PASS |
| TC-HIRE-03 | Hire, tool unavailable | `Cap_Hire` → unavailable | Customer cancelled | PASS |

**10 / 10 journeys COMPLETED — 0 stuck, 0 incident.**

All four DMN decision tables were evaluated during the run (14 evaluation instances visible in
Operate → Decisions: Finance credit assessment, Loyalty discount band, Tiered hire pricing, FixPro
batch triage). See `docs/evidence/dmn-decisions-evaluated.png`.

## Representative evidence (H2 assertions)

**TC-BUY-02 — buy with finance approved**
```
ORDERS:            ORD-… DISPATCHED, £129.90
FINANCE_TRANSFERS: FIN-… APPROVED, £129.90, term 6 (finance_assessment DMN → APPROVE)
TRADE_CARDS:       TC-SILVER01 +649 points (loyalty accrual)
```

**TC-HIRE-01 — hire with FixPro maintenance**
```
RENTALS:           BK-… PWH-2500 £67.50, tier "2-3 days"  (hire_pricing DMN → 0.90 multiplier)
SERVICE_RECORDS:   SRV-… REPORTED, routine 1 / repair 0   (maintenance_triage DMN)
SERVICE_INVOICES:  INV-… £47.50  (labour £12.50 + call-out £35.00)
```

**TC-BUY-03 — finance declined (credit score 400)**
```
FINANCE_TRANSFERS: FIN-REJ-… REJECTED   (finance_assessment DMN → DECLINE)
No ORDERS row; journey ends at "Customer cancelled".
```

Final database state after the suite: 3 orders, 3 finance transfers (2 APPROVED / 1 REJECTED),
3 rentals, 2 service invoices, 5 trade cards.

## Operate evidence

| File | Shows |
|---|---|
| `docs/evidence/buy-journey-collaboration.png` | Buy-with-finance: Customer + FinTrust pools, 3 call activities, message flows |
| `docs/evidence/hire-journey-3pools.png` | Hire-with-maintenance: Customer + FinTrust + FixPro pools |
| `docs/evidence/fixpro-partner.png` | FixPro partner: collect → triage (DMN) → service → report → return |
| `docs/evidence/dmn-decisions-evaluated.png` | Operate Decisions tab: all four DMN tables evaluated |

## Defects / observations

No open defects. Every cancellation path (backorder, quote rejected, finance declined, tool
unavailable) reaches a clean *Customer cancelled* end event — the orchestration design has no dead
ends.

**Sign-off:** MA — 13 July 2026

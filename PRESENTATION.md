# ProBuild Operations Platform — Presentation Script

_Case study · Camunda 8 · iStar 2.0 · **Orchestration architecture**_
UFCFAF-30-3 Development of Information Systems Project · UWE Bristol · MA

> Video structure: **i\* goals → architecture → demo each journey → tie every demo moment back to
> an i\* element → testing candour → reflection.** Target under 20 minutes. Everything below is
> backed by the running system and the evidence in `docs/evidence/`.

---

## 1 · The client

ProBuild Supplies Ltd — a builders' merchant running three services (retail **Buy**, **Tool Hire**,
loyalty **Trade Card**) that must span the customer, ProBuild, and two external partners (**FinTrust**
finance, **FixPro** maintenance) as *one automated process* — no manual re-keying, every step
auditable.

## 2 · Method — one chain, model to running system

**i\* socio-technical model → orchestrator + capabilities → partner choreography → DMN decisions →
verifiable evidence.** Each layer fulfils a labelled element of the one above. Whenever a token
moves, I can name the i\* element it satisfies (see `docs/istar-model.md` §4).

## 3 · Socio-technical model (i\*)

Nine actors. ProBuild is modelled as an **orchestrator that depends on internal capability agents**
(Sales, Warehouse, Loyalty, Tool Hire) and reaches the two **external agents** (FinTrust, FixPro)
only by message. Softgoal-conflict analysis drives the design — e.g. *fast decision* vs *responsible
lending* is resolved by a DMN recommendation confirmed by a human. (Full justification:
`docs/istar-model.md`.)

## 4 · Architecture — orchestration, not a monolith

- **`Journey_Customer`** — a lean orchestrator (3-pool collaboration: Customer + FinTrust + FixPro)
- **4 capability processes** invoked as **call activities**: `Cap_Sales`, `Cap_Warehouse`,
  `Cap_Loyalty`, `Cap_Hire` — independent, reusable (Warehouse serves *both* buy and hire)
- **2 partner processes** reached by **correlated messages**: `Partner_FinTrust`, `Partner_FixPro`
  (+ a **timer-triggered** weekly maintenance sweep)
- **4 DMN decision tables** carry the business rules — no `if` statements in Java
- **24 workers**, **8 H2 tables** of evidence

_This is the key departure from a lane-based collaboration: capabilities are separately deployable
and testable, and the external-partner boundary is explicit._

## 5 · Journey 1 — Buy with finance (demo)

`docs/evidence/buy-journey-collaboration.png` — one screenshot shows it all:
Browse → **`Cap_Sales`** (quote) → review → pay (finance) → **`Partner_FinTrust`** pool:
`finance_assessment` **DMN** recommends, a human confirms, funds transfer, message back →
**`Cap_Warehouse`** (pick/QC/dispatch) → **`Cap_Loyalty`** (accrue) → served.
**Watch:** the same `journeyId` correlates Customer ↔ FinTrust; H2 shows ORDER DISPATCHED, finance
APPROVED, points accrued.
**Ties to i\*:** *smooth experience*, *finance decision*, *responsible lending*, *order fulfilled*.

## 6 · Journey 2 — Hire with maintenance (demo)

`docs/evidence/hire-journey-3pools.png` — three pools:
Check availability → **`Cap_Hire`** (`hire_pricing` **DMN** → tiered fee) → deposit → use → return →
**`Cap_Warehouse`** (inspect) → needs service → **`Partner_FixPro`** pool: collect →
`maintenance_triage` **DMN** → service → PAT → invoice → message back → ready to hire.
**Ties to i\*:** *fair transparent hire pricing*, *tools safe & compliant*, *minimise downtime*.

## 7 · Journey 3 — Trade Card + the paths that aren't happy

Card issue/renew → `Cap_Loyalty` + `loyalty_band` DMN. And every non-happy path is handled — out of
stock, quote rejected, finance declined, tool unavailable **all reach a clean *Customer cancelled*
end**. There are no dead ends (this fixes a real flaw in the lane-based design).

## 8 · The decision layer (DMN)

`docs/evidence/dmn-decisions-evaluated.png` — Operate's Decisions tab shows all four tables
evaluating in real runs: Finance credit assessment, Loyalty discount band, Tiered hire pricing,
FixPro batch triage. Business rules are inspectable and uniform — the "fair, transparent" softgoals
are structurally guaranteed.

## 9 · Testing — honestly

10 journey paths driven through the engine's REST API, **10/10 completed**, asserted three ways
(end event + capability/partner completion + H2 rows). All four DMN tables and both partner message
round-trips exercised. See `TEST_REPORT.md`.

## 10 · Reflection

**What worked:** model-to-runtime traceability — every i\* element became a verifiable artefact;
externalising rules into DMN kept the softgoals honest; message choreography kept partners
autonomous yet coordinated. **What I'd extend:** compensation/SAGA to auto-void a financed order if
later declined; schedule-test the weekly FixPro timer; PostgreSQL + Flyway for production
persistence.

> _"The i\* model is not documentation — it is the contract the orchestration fulfils."_

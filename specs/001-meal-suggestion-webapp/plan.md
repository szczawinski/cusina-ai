# Implementation Plan: Cusina AI — Meal Suggestion Web Application

**Branch**: `001-meal-suggestion-webapp` | **Date**: 2026-06-13 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-meal-suggestion-webapp/spec.md`

---

## Summary

Plan covers a server-rendered web app (Java 21, Spring Boot 3, Thymeleaf, Maven) that generates meal suggestions from ingredients using Anthropic Claude API, with three clarified business anchors: (1) session starts with exactly 10 random unique ingredients, (2) request form exposes exactly 2 comboboxes with closed canonical options, and (3) a suggestion is valid even if it uses only a subset of provided ingredients.

---

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot MVC + Thymeleaf + Validation, anthropic-java client, JUnit 5/Mockito  
**Storage**: HTTP session only (no DB)  
**Testing**: WebMvc tests, service tests, manual viewport checks (320–430 px)  
**Target Platform**: Local JAR + containerized runtime (Cloud Run compatible)  
**Project Type**: Server-rendered web application

**Functional constraints**:
- Session bootstrap must preload exactly 10 random unique ingredients.
- Ingredient list max 50, deduplicated case-insensitive (first value wins).
- Meal request has optional free-text preferences (max 500 chars).
- Two optional comboboxes with strict closed values:
  - dishType: `śniadanie | obiad | deser`
  - dietType: `lekka | śródziemnomorska | wegetariańska`
- Out-of-list combobox values are rejected server-side and must not trigger AI call.
- Successful AI payload must contain exactly 3 raw suggestions.
- Malformed suggestions are dropped; valid ones shown with warning.
- Suggestion validity allows non-empty subset usage of available ingredients.
- UI and presented content remain Polish-only.

**NEEDS CLARIFICATION**: None

---

## Constitution Check

Constitution file exists but remains template-only with no enforceable project principles. No constitutional blockers identified.

| Check | Status | Notes |
|-------|--------|-------|
| Placeholder constitution has no enforceable gates | ✅ PASS | Planning gates applied from feature spec |
| Clarified startup ingredients rule represented | ✅ PASS | Exact random 10 included |
| Clarified combobox domains represented | ✅ PASS | Two closed enums included |
| Clarified subset-ingredient validity represented | ✅ PASS | Included in validation model |

---

## Project Structure

```text
specs/001-meal-suggestion-webapp/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── http-routes.md
└── tasks.md
```

```text
src/main/java/com/cusina/ai/
├── controller/
├── model/
├── service/
├── session/
└── config/

src/main/resources/
├── templates/
└── static/css/
```

---

## Phase 0 Output (Research)

`research.md` resolves implementation decisions for:
1. deterministic handling of 10-random-ingredient bootstrap,
2. strict validation strategy for 2 closed-list comboboxes,
3. subset-ingredient acceptance in results validation,
4. exact-3 and malformed-item handling policy.

---

## Phase 1 Outputs (Design & Contracts)

- `data-model.md`: updated entities/validation for startup preload, combobox enums, subset usage.
- `contracts/http-routes.md`: updated request/validation and route contracts.
- `quickstart.md`: updated validation scenarios and test commands.
- `.github/copilot-instructions.md`: plan reference verified between markers.

---

## Post-Design Constitution Check

| Check | Status | Notes |
|-------|--------|-------|
| All clarifications mapped into design artifacts | ✅ PASS | Startup preload, combobox constraints, subset validity included |
| No unresolved unknowns | ✅ PASS | Research closed all decisions |
| Contract and data model alignment | ✅ PASS | Same enums/rules in both artifacts |

---

## Complexity Tracking

No constitution violations and no exceptions required.

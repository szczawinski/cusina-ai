---
description: "Actionable, dependency-ordered tasks for Cusina AI meal suggestion webapp"
---

# Tasks: Cusina AI — Meal Suggestion Web Application

**Input**: Design documents from `/specs/001-meal-suggestion-webapp/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/http-routes.md, quickstart.md

**Tests**: Included because spec.md defines independent test criteria and quickstart.md defines validation scenarios.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare project baseline for Spring MVC + Thymeleaf + Anthropic integration and Polish-first UI.

- [X] T001 Align dependencies/plugins for Spring Boot MVC, Thymeleaf, Validation, Actuator, Anthropic SDK, and JUnit/Mockito in pom.xml
- [X] T002 Configure default runtime properties (locale, encoding, timeout placeholders, validation messages) in src/main/resources/application.properties
- [X] T003 [P] Prepare shared Polish layout shell and flash-message fragment in src/main/resources/templates/fragments/layout.html
- [X] T004 [P] Establish mobile-first premium design tokens and base styles in src/main/resources/static/css/style.css
- [X] T005 [P] Configure Cloud Run friendly profile defaults in src/main/resources/application-cloud.properties

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build shared domain and integration primitives required before user stories.

**⚠️ CRITICAL**: No user story work starts before this phase is complete.

- [X] T006 Implement Anthropic client configuration and API key wiring in src/main/java/com/cusina/ai/config/AnthropicConfig.java
- [X] T007 [P] Implement fail-fast startup validation for `ANTHROPIC_API_KEY` in src/main/java/com/cusina/ai/config/AppStartupValidator.java
- [X] T008 [P] Create canonical enums for `dishType` and `dietType` in src/main/java/com/cusina/ai/model/DishType.java and src/main/java/com/cusina/ai/model/DietType.java
- [X] T009 Create shared request/response domain models (`MealRequest`, `MealSuggestion`, `MealResponse`) in src/main/java/com/cusina/ai/model/
- [X] T010 [P] Add global Polish validation and exception handling in src/main/java/com/cusina/ai/controller/GlobalExceptionHandler.java
- [X] T011 [P] Add baseline config/health tests for startup and actuator endpoints in src/test/java/com/cusina/ai/config/StartupAndHealthTest.java

**Checkpoint**: Foundation complete; user stories can be implemented independently.

---

## Phase 3: User Story 1 - Manage Ingredient List (Priority: P1) 🎯 MVP

**Goal**: Session starts with exactly 10 random unique ingredients; user can add/remove/deduplicate ingredients in Polish UI.

**Independent Test**: Start new session on `/ingredients`, verify exactly 10 unique preloaded ingredients, add and remove entries, verify duplicate add is ignored and list persists within session.

### Tests for User Story 1

- [X] T012 [P] [US1] Add unit tests for `drawUnique(10)` and duplicate-resistant preload behavior in src/test/java/com/cusina/ai/session/IngredientPoolTest.java
- [X] T013 [P] [US1] Add unit tests for session initialization, case-insensitive deduplication, and 50-item cap in src/test/java/com/cusina/ai/session/IngredientSessionTest.java
- [X] T014 [P] [US1] Add WebMvc tests for first-load preload=10, add/remove PRG flow, and Polish feedback in src/test/java/com/cusina/ai/controller/IngredientControllerTest.java

### Implementation for User Story 1

- [X] T015 [P] [US1] Implement curated ingredient source and `drawUnique(count)` logic in src/main/java/com/cusina/ai/session/IngredientPool.java
- [X] T016 [US1] Implement `Ingredient` and `IngredientSession.initializeIfNeeded()` with exact-10 preload rule in src/main/java/com/cusina/ai/model/Ingredient.java and src/main/java/com/cusina/ai/session/IngredientSession.java
- [X] T017 [P] [US1] Implement ingredient add/remove form DTO and Polish validation messages in src/main/java/com/cusina/ai/controller/form/IngredientForm.java and src/main/resources/messages.properties
- [X] T018 [US1] Implement ingredient routes (`/`, `/ingredients`, `/ingredients/add`, `/ingredients/remove`) with preload trigger in src/main/java/com/cusina/ai/controller/IngredientController.java
- [X] T019 [US1] Build ingredients page with preloaded-list presentation, empty-state helper, and remove controls in src/main/resources/templates/ingredients.html

**Checkpoint**: US1 is independently functional and testable.

---

## Phase 4: User Story 2 - Request Meal Suggestions (Priority: P2)

**Goal**: User submits meal request with optional free text and exactly two comboboxes; out-of-list values are rejected and selected values influence AI prompt.

**Independent Test**: With pre-populated ingredients, submit `/meal-request` using valid/invalid combobox values; confirm invalid payload stays on form with Polish errors and no AI call; valid selections are passed to service and affect prompt context.

### Tests for User Story 2

- [X] T020 [P] [US2] Add form validation tests for closed-list `dishType`/`dietType` and max-length dietary preferences in src/test/java/com/cusina/ai/controller/form/MealRequestFormTest.java
- [X] T021 [P] [US2] Add WebMvc tests for meal-request guard, combobox validation errors, and no-AI-call on tampered values in src/test/java/com/cusina/ai/controller/MealControllerTest.java
- [X] T022 [P] [US2] Add service test verifying prompt includes selected `dishType` and `dietType` context when provided in src/test/java/com/cusina/ai/service/MealSuggestionServiceTest.java

### Implementation for User Story 2

- [X] T023 [P] [US2] Implement meal request form DTO with optional fields, 500-char cap, and enum-backed combobox validation in src/main/java/com/cusina/ai/controller/form/MealRequestForm.java
- [X] T024 [US2] Implement `/meal-request` and `/meal-request/suggest` orchestration with server-side validation gate before AI call in src/main/java/com/cusina/ai/controller/MealController.java
- [X] T025 [US2] Build meal-request template with exactly two select controls (`śniadanie|obiad|deser`, `lekka|śródziemnomorska|wegetariańska`) in src/main/resources/templates/meal-request.html
- [X] T026 [US2] Update prompt builder to include ingredients, free text, and selected combobox values as explicit AI constraints in src/main/java/com/cusina/ai/service/MealSuggestionService.java
- [X] T027 [US2] Add/adjust Polish labels and validation strings for combobox domains and tamper rejection in src/main/resources/messages.properties

**Checkpoint**: US2 is independently functional and testable.

---

## Phase 5: User Story 4 - Experience Premium Polish UI on iPhone (Priority: P2)

**Goal**: Ingredient, request, and results screens remain premium, readable, and touch-friendly on iPhone Safari 320–430 px.

**Independent Test**: Validate `/ingredients`, `/meal-request`, `/results` at 320/375/390/414/430 px in portrait/landscape with no horizontal scrolling and usable primary actions.

### Tests for User Story 4

- [X] T028 [P] [US4] Extend manual viewport acceptance matrix for 320–430 px and orientation checks in specs/001-meal-suggestion-webapp/quickstart.md
- [X] T029 [P] [US4] Add premium-minimalist visual QA checklist for typography, spacing, and depth cues in specs/001-meal-suggestion-webapp/quickstart.md

### Implementation for User Story 4

- [X] T030 [US4] Apply mobile-first premium layout refinements and overflow guards in src/main/resources/static/css/style.css
- [X] T031 [P] [US4] Refine ingredients view spacing and touch targets for narrow widths in src/main/resources/templates/ingredients.html
- [X] T032 [P] [US4] Refine meal-request form layout for combobox readability and error message wrapping on iPhone widths in src/main/resources/templates/meal-request.html
- [X] T033 [P] [US4] Refine results card layout for stacked mobile readability in src/main/resources/templates/results.html

**Checkpoint**: US4 is independently testable via iPhone Safari quality matrix.

---

## Phase 6: User Story 3 - View AI-Generated Meal Suggestions (Priority: P3)

**Goal**: Results honor exact-3 policy, filter malformed suggestions, and accept suggestions using any non-empty subset of provided ingredients.

**Independent Test**: Mock AI responses for exact-3 success, malformed partial success, invalid-count failure, and subset-ingredient usage; verify Polish warning/error/result rendering.

### Tests for User Story 3

- [X] T034 [P] [US3] Add service tests for raw-count rule (`==3` success, otherwise retry error) in src/test/java/com/cusina/ai/service/MealSuggestionServiceTest.java
- [X] T035 [P] [US3] Add service tests for malformed suggestion omission and warning message behavior in src/test/java/com/cusina/ai/service/MealSuggestionServiceTest.java
- [X] T036 [P] [US3] Add service tests for subset-ingredient validity (non-empty subset accepted, out-of-scope ingredients rejected) in src/test/java/com/cusina/ai/service/MealSuggestionServiceTest.java
- [X] T037 [P] [US3] Add WebMvc tests for results success/warning/error rendering and retry navigation in src/test/java/com/cusina/ai/controller/MealControllerTest.java

### Implementation for User Story 3

- [X] T038 [US3] Implement response validator for exact-3, malformed filtering, and subset acceptance rule in src/main/java/com/cusina/ai/service/MealSuggestionService.java
- [X] T039 [US3] Implement results route/session handoff for success, warning, and retry states in src/main/java/com/cusina/ai/controller/MealController.java
- [X] T040 [US3] Build Polish results and retry/error templates in src/main/resources/templates/results.html and src/main/resources/templates/error.html

**Checkpoint**: US3 is independently functional and testable.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final hardening, docs alignment, and end-to-end verification.

- [X] T041 [P] Synchronize quickstart command and scenario checklist with implemented flow in specs/001-meal-suggestion-webapp/quickstart.md
- [X] T042 [P] Update route contract details if implementation deltas exist (validation messages, redirects, guards) in specs/001-meal-suggestion-webapp/contracts/http-routes.md
- [X] T043 Execute full regression for US1-US4 scenarios and record outcomes in specs/001-meal-suggestion-webapp/quickstart.md
- [X] T044 [P] Finalize Cloud Run deployment script/env wiring for runtime parity in scripts/deploy-cloud-run.sh
- [X] T045 [P] Add CI verification step for tests and packaging in .github/workflows/cloud-run.yml

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No prerequisites.
- **Phase 2 (Foundational)**: Depends on Phase 1; blocks all user stories.
- **Phase 3 (US1)**: Depends on Phase 2.
- **Phase 4 (US2)**: Depends on US1 session ingredient flow + Phase 2.
- **Phase 5 (US4)**: Depends on Phase 2 and can run in parallel with US2/US3 implementation.
- **Phase 6 (US3)**: Depends on US2 submission + AI integration path.
- **Phase 7 (Polish)**: Depends on completion of target stories.

### User Story Dependency Graph

- `US1 -> US2 -> US3`
- `US4` depends on shared templates/styles and reaches full acceptance after `US1 + US2 + US3`.

### Within Each User Story

- Tests first (fail), then model/service/controller, then templates/integration.

---

## Parallel Opportunities

- **Setup**: T003, T004, T005 can run in parallel.
- **Foundational**: T007, T008, T010, T011 can run in parallel after T006/T009.
- **US1**: T012, T013, T014 parallel; T015 and T017 parallel before controller/template wiring.
- **US2**: T020, T021, T022 parallel; T023 and T025 parallel before final orchestration.
- **US4**: T028, T029, T031, T032, T033 parallel with CSS coordination.
- **US3**: T034, T035, T036, T037 parallel before T038/T039/T040.
- **Polish**: T041, T042, T044, T045 parallel.

---

## Parallel Example: User Story 1

```bash
Task: T012 [US1] IngredientPool unique preload tests in src/test/java/com/cusina/ai/session/IngredientPoolTest.java
Task: T013 [US1] IngredientSession rules tests in src/test/java/com/cusina/ai/session/IngredientSessionTest.java
Task: T015 [US1] IngredientPool implementation in src/main/java/com/cusina/ai/session/IngredientPool.java
```

## Parallel Example: User Story 2

```bash
Task: T020 [US2] MealRequestForm closed-list validation tests in src/test/java/com/cusina/ai/controller/form/MealRequestFormTest.java
Task: T021 [US2] MealController tampered payload tests in src/test/java/com/cusina/ai/controller/MealControllerTest.java
Task: T025 [US2] meal-request combobox template in src/main/resources/templates/meal-request.html
```

## Parallel Example: User Story 3

```bash
Task: T034 [US3] raw-count rule tests in src/test/java/com/cusina/ai/service/MealSuggestionServiceTest.java
Task: T035 [US3] malformed omission tests in src/test/java/com/cusina/ai/service/MealSuggestionServiceTest.java
Task: T036 [US3] subset-ingredient acceptance tests in src/test/java/com/cusina/ai/service/MealSuggestionServiceTest.java
```

## Parallel Example: User Story 4

```bash
Task: T028 [US4] viewport matrix update in specs/001-meal-suggestion-webapp/quickstart.md
Task: T031 [US4] ingredients mobile refinements in src/main/resources/templates/ingredients.html
Task: T032 [US4] meal-request mobile refinements in src/main/resources/templates/meal-request.html
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1).
3. Validate US1 independently on `/ingredients` (including exact preload=10).
4. Demo MVP.

### Incremental Delivery

1. Setup + Foundational baseline.
2. Deliver US1 (session ingredients + random preload).
3. Deliver US2 (combobox validation + prompt influence).
4. Deliver US3 (exact-3 + malformed + subset acceptance).
5. Deliver US4 quality gate (premium iPhone UX).
6. Finish Polish phase for release readiness.

### Notes

- All tasks use strict checklist format with explicit file paths.
- Story labels are only used in user-story phases.
- Requested clarifications are explicitly covered: exact 10 random preload, combobox closed-list validation with prompt impact, subset ingredient acceptance in AI result validation.

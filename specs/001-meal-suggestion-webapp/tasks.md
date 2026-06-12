---
description: "Actionable, dependency-ordered tasks for Cusina AI meal suggestion webapp"
---

# Tasks: Cusina AI — Meal Suggestion Web Application

**Input**: Design documents from `/specs/001-meal-suggestion-webapp/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/http-routes.md, quickstart.md

**Tests**: Included because spec.md defines mandatory user scenarios/testing and quickstart.md defines automated/manual validation targets.

**Organization**: Tasks are grouped by user story to support independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize Spring Boot MVC project scaffolding, dependencies, and baseline web assets.

- [X] T001 Update Maven dependencies/plugins for Spring Boot MVC, Thymeleaf, Validation, Anthropic SDK, and test stack in pom.xml
- [X] T002 Create Spring Boot entrypoint and package structure in src/main/java/com/cusina/ai/CusinaAiApplication.java
- [X] T003 [P] Add base application configuration (server, Anthropic defaults, Jackson settings) in src/main/resources/application.properties
- [X] T004 [P] Add base Thymeleaf layout shell in src/main/resources/templates/fragments/layout.html
- [X] T005 [P] Add responsive warm-palette baseline styles in src/main/resources/static/css/style.css

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build shared infrastructure required by all user stories.

**⚠️ CRITICAL**: Complete this phase before starting user story implementation.

- [X] T006 Create Anthropic client bean configuration in src/main/java/com/cusina/ai/config/AnthropicConfig.java
- [X] T007 [P] Create async executor configuration for AI calls in src/main/java/com/cusina/ai/config/AsyncConfig.java
- [X] T008 [P] Implement fail-fast API key startup validation in src/main/java/com/cusina/ai/config/AppStartupValidator.java
- [X] T009 Create shared model classes (MealRequest, MealSuggestion, MealResponse) in src/main/java/com/cusina/ai/model/
- [X] T010 [P] Implement AI service with prompt builder, JSON parsing, and timeout-safe async contract in src/main/java/com/cusina/ai/service/MealSuggestionService.java
- [X] T011 [P] Add service-level unit tests for JSON parse and AI error paths in src/test/java/com/cusina/ai/service/MealSuggestionServiceTest.java

**Checkpoint**: Shared infrastructure, AI integration scaffolding, and core models are ready.

---

## Phase 3: User Story 1 - Manage Ingredient List (Priority: P1) 🎯 MVP

**Goal**: Users can add, deduplicate, remove, and retain ingredients within session state.

**Independent Test**: Add/remove ingredients on `/ingredients`, verify empty state, session persistence, and case-insensitive duplicate ignore with first-write-wins behavior.

### Tests for User Story 1

- [X] T012 [P] [US1] Add unit tests for case-insensitive first-write-wins deduplication and 50-item cap in src/test/java/com/cusina/ai/session/IngredientSessionTest.java
- [X] T013 [P] [US1] Add WebMvc tests for ingredient add/remove/empty-state flows in src/test/java/com/cusina/ai/controller/IngredientControllerTest.java

### Implementation for User Story 1

- [X] T014 [US1] Implement Ingredient value object with normalized key behavior in src/main/java/com/cusina/ai/model/Ingredient.java
- [X] T015 [US1] Implement session-scoped ingredient state manager with case-insensitive first-write-wins dedup logic in src/main/java/com/cusina/ai/session/IngredientSession.java
- [X] T016 [US1] Add ingredient form DTO with validation constraints in src/main/java/com/cusina/ai/controller/form/IngredientForm.java
- [X] T017 [US1] Implement ingredient routes (`/`, `/ingredients`, `/ingredients/add`, `/ingredients/remove`) and flash messaging in src/main/java/com/cusina/ai/controller/IngredientController.java
- [X] T018 [US1] Build ingredient management view with list, count, messages, and remove actions in src/main/resources/templates/ingredients.html

**Checkpoint**: User Story 1 is independently functional and testable (MVP pantry tracker).

---

## Phase 4: User Story 2 - Request Meal Suggestions (Priority: P2)

**Goal**: Users can review session ingredients, enter optional dietary preferences, and submit valid suggestion requests.

**Independent Test**: With preloaded session ingredients, submit `/meal-request/suggest` with and without dietary preferences; verify empty-list guard and 500-char validation behavior.

### Tests for User Story 2

- [X] T019 [P] [US2] Add WebMvc tests for meal-request guard, form validation, and submit flow in src/test/java/com/cusina/ai/controller/MealControllerTest.java
- [X] T020 [P] [US2] Add unit tests for meal request form constraints in src/test/java/com/cusina/ai/model/MealRequestValidationTest.java

### Implementation for User Story 2

- [X] T021 [US2] Implement meal request page route and empty-ingredient guard in src/main/java/com/cusina/ai/controller/MealController.java
- [X] T022 [US2] Implement submit handler to validate input, invoke async service, and redirect with flash MealResponse in src/main/java/com/cusina/ai/controller/MealController.java
- [X] T023 [US2] Build meal request form view with ingredient review, textarea, and validation rendering in src/main/resources/templates/meal-request.html
- [X] T024 [US2] Add dietary preference length and timeout properties wiring in src/main/resources/application.properties

**Checkpoint**: User Stories 1 and 2 work end-to-end up to request submission.

---

## Phase 5: User Story 3 - View AI-Generated Meal Suggestions (Priority: P3)

**Goal**: Users see validated suggestion results, warnings for omitted malformed items, and retry UX for invalid or failed responses.

**Independent Test**: Mock AI responses to verify: exactly 3 raw suggestions required, malformed suggestions dropped with warning, valid suggestions rendered, and error/retry flows shown.

### Tests for User Story 3

- [X] T025 [P] [US3] Add service tests for exact-3 raw count validation and invalid-count error mapping in src/test/java/com/cusina/ai/service/MealSuggestionServiceTest.java
- [X] T026 [P] [US3] Add service tests for malformed suggestion filtering and warning count/message in src/test/java/com/cusina/ai/service/MealSuggestionServiceTest.java
- [X] T027 [P] [US3] Add WebMvc tests for results success/warning/error rendering and redirect guard in src/test/java/com/cusina/ai/controller/MealControllerTest.java

### Implementation for User Story 3

- [X] T028 [US3] Implement response validator to enforce exactly 3 raw suggestions before filtering in src/main/java/com/cusina/ai/service/MealSuggestionService.java
- [X] T029 [US3] Implement malformed suggestion filtering (blank name/description/steps) and omitted warning metadata in src/main/java/com/cusina/ai/service/MealSuggestionService.java
- [X] T030 [US3] Implement results route guard and error/warning model handling in src/main/java/com/cusina/ai/controller/MealController.java
- [X] T031 [US3] Build results view for meal cards, omission warning banner, and retry/start-over controls in src/main/resources/templates/results.html
- [X] T032 [US3] Add user-friendly global error page template for unexpected 4xx/5xx states in src/main/resources/templates/error.html

**Checkpoint**: All user stories are independently functional and validated.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final quality, UX consistency, and release readiness checks.

- [X] T033 [P] Update prompt wording and service comments to align with clarified rules (exactly 3 suggestions, malformed drop warning) in src/main/java/com/cusina/ai/service/MealSuggestionService.java
- [X] T034 [P] Refine responsive layout/accessibility polish across ingredient, request, and result views in src/main/resources/templates/ and src/main/resources/static/css/style.css
- [X] T035 Execute quickstart validation scenarios and record outcomes in specs/001-meal-suggestion-webapp/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1; blocks all user stories.
- **Phase 3 (US1)**: Depends on Phase 2; establishes MVP.
- **Phase 4 (US2)**: Depends on Phase 2 and integrates with US1 session ingredient state.
- **Phase 5 (US3)**: Depends on Phase 2 and consumes US2 submit flow and service outputs.
- **Phase 6 (Polish)**: Depends on completion of target user stories.

### User Story Dependency Graph

- **US1 (P1)** → required for ingredient session behavior.
- **US2 (P2)** → depends on US1 ingredient list availability for meaningful submission.
- **US3 (P3)** → depends on US2 request execution and service response handling.

Graph: `US1 -> US2 -> US3`

### Within Each User Story

- Tests first (where listed), then model/session/service logic, then controllers/routes, then templates.

---

## Parallel Opportunities

- **Setup**: T003, T004, T005 can run in parallel after T001/T002.
- **Foundational**: T007, T008, T010, T011 can run in parallel after core skeleton is available.
- **US1**: T012 and T013 run in parallel; T018 can begin once controller contracts stabilize.
- **US2**: T019 and T020 run in parallel; T023 and T024 can run in parallel with controller work.
- **US3**: T025, T026, T027 run in parallel; T031 and T032 can run in parallel with service/controller updates.
- **Polish**: T033 and T034 run in parallel.

---

## Parallel Example: User Story 1

```bash
# Parallel test work
Task: T012 [US1] IngredientSession dedup/cap unit tests
Task: T013 [US1] IngredientController WebMvc tests

# Parallel implementation after session/controller contracts exist
Task: T016 [US1] IngredientForm DTO
Task: T018 [US1] ingredients.html view
```

## Parallel Example: User Story 2

```bash
# Parallel tests
Task: T019 [US2] MealController WebMvc tests
Task: T020 [US2] MealRequest validation tests

# Parallel implementation
Task: T023 [US2] meal-request.html
Task: T024 [US2] property wiring
```

## Parallel Example: User Story 3

```bash
# Parallel validation tests
Task: T025 [US3] exact-3 count validation tests
Task: T026 [US3] malformed filtering/warning tests
Task: T027 [US3] results rendering tests

# Parallel UI + error page work
Task: T031 [US3] results.html
Task: T032 [US3] error.html
```

---

## Implementation Strategy

### MVP First (US1 only)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1).
3. Validate ingredient tracker behavior (including case-insensitive first-write-wins dedup).
4. Demo/release MVP.

### Incremental Delivery

1. Foundation complete (Phases 1–2).
2. Deliver US1 (ingredient management).
3. Deliver US2 (request submission).
4. Deliver US3 (validated results with warning/error behaviors).
5. Finish polish and quickstart validation.

### Notes

- Every task includes explicit file path(s) and is executable without hidden context.
- Clarified behaviors are explicitly covered: exact raw count of 3 required, malformed suggestions dropped with warning, and case-insensitive first-write-wins ingredient deduplication.


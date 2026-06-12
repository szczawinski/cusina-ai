# Quickstart & Validation Guide: Cusina AI

**Feature**: `001-meal-suggestion-webapp`  
**Phase**: 1 — Design  
**Date**: 2025-07-22

---

## Purpose

This guide documents how to build, run, and manually validate the Cusina AI application end-to-end. It covers prerequisites, startup commands, and concrete validation scenarios mapped to each acceptance requirement from the spec.

For entity definitions see [data-model.md](./data-model.md). For route details see [contracts/http-routes.md](./contracts/http-routes.md).

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21 | `java -version` must show 21.x |
| Maven | 3.8+ | `mvn -version` |
| Anthropic API Key | — | Obtain from [console.anthropic.com](https://console.anthropic.com); set as `ANTHROPIC_API_KEY` environment variable |

---

## Setup & Run

### 1. Clone and build

```bash
# From the repository root
mvn clean package -DskipTests
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: ~20s
```

Artifact: `target/cusina-ai-0.0.1-SNAPSHOT.jar`

### 2. Set API key

```bash
# Mac / Linux
export ANTHROPIC_API_KEY=sk-ant-api03-...

# Windows PowerShell
$env:ANTHROPIC_API_KEY = "sk-ant-api03-..."

# Windows CMD
set ANTHROPIC_API_KEY=sk-ant-api03-...
```

### 3. Start the application

```bash
java -jar target/cusina-ai-0.0.1-SNAPSHOT.jar
```

Expected startup log lines:
```
Started CusinaAiApplication in ~3s
Tomcat started on port 8080
```

Application is available at: **http://localhost:8080**

### 4. Fail-fast validation (missing API key)

```bash
# Unset the key first
unset ANTHROPIC_API_KEY   # Mac/Linux
# or $env:ANTHROPIC_API_KEY = "" (PowerShell)

java -jar target/cusina-ai-0.0.1-SNAPSHOT.jar
```

Expected log:
```
[ERROR] ANTHROPIC_API_KEY environment variable is not set. 
        Application cannot start without a valid API key.
        Set ANTHROPIC_API_KEY and restart.
```
Expected: application exits with code `1`. *(Validates SC-008, FR-009)*

---

## Validation Scenarios

### Scenario 1 — Empty Ingredient List State (FR-003, User Story 1 Acceptance #3)

1. Open **http://localhost:8080/ingredients**
2. **Verify**: Page shows a helpful prompt (e.g., *"Add ingredients from your fridge or pantry to get started."*)
3. **Verify**: The ingredient list area is empty; no list items rendered
4. **Verify**: The add form input and button are visible and accessible

---

### Scenario 2 — Add Ingredients (FR-001, User Story 1 Acceptance #1)

1. On **/ingredients**, type `"Chicken breast"` in the add field and submit
2. **Verify**: Page reloads (PRG redirect); `"Chicken breast"` appears in the ingredient list
3. **Verify**: Item count shows `1 / 50`
4. Add `"Garlic"`, `"Olive oil"`, `"Lemon"`
5. **Verify**: All 4 items appear in the list in insertion order

*Expected time from add to display: < 1 second (SC-001)*

---

### Scenario 3 — Remove an Ingredient (FR-002, User Story 1 Acceptance #2)

1. With at least 2 ingredients in the list, click the remove button next to one ingredient
2. **Verify**: Page reloads; that ingredient is no longer in the list
3. **Verify**: Remaining ingredients are still present

---

### Scenario 4 — Duplicate Ingredient Prevention (Edge Case)

1. Add `"Chicken breast"` to the list
2. Attempt to add `"chicken breast"` (different case) again
3. **Verify**: An error message appears (e.g., *"'chicken breast' is already in your list."*)
4. **Verify**: The list still contains only one chicken entry

---

### Scenario 5 — Session Persistence (User Story 1 Acceptance #4)

1. Add several ingredients on **/ingredients**
2. Navigate to **/meal-request**
3. Click "Edit Ingredients" link back to **/ingredients**
4. **Verify**: The ingredient list from step 1 is still present

---

### Scenario 6 — Ingredient Cap (FR-010, Edge Case)

1. Add ingredients until the list reaches **50 items**
   *(Tip: use a browser console script or rapidly submit the form)*
2. **Verify**: Item count shows `50 / 50`
3. **Verify**: The add form input or button is disabled; a message indicates the list is full
4. Attempt to submit the form anyway (e.g., via browser dev tools)
5. **Verify**: Server rejects the add and returns a message; the list remains at 50 items

---

### Scenario 7 — Empty Ingredient List Blocks Submission (FR-005, User Story 2 Acceptance #4)

1. Clear all ingredients (remove all one by one, or open a fresh browser session)
2. Navigate directly to **http://localhost:8080/meal-request**
3. **Verify**: Redirected back to **/ingredients** with message *"Please add at least one ingredient before requesting meal suggestions."*

---

### Scenario 8 — Meal Request with Dietary Preferences (FR-004, FR-006, User Story 2 Acceptance #1–3)

1. Add at least 3 ingredients: `"Eggs"`, `"Spinach"`, `"Feta cheese"`
2. Navigate to **/meal-request**
3. **Verify**: The current ingredient list is displayed for review
4. In the dietary preferences field, type `"vegetarian"`
5. Click **"Get Meal Suggestions"**
6. **Verify**: A loading state or response begins (no error page)
7. **Verify**: Within 15 seconds (SC-003), the page redirects to **/results**

---

### Scenario 9 — View Meal Suggestions (FR-007, FR-014, User Story 3 Acceptance #1, #4)

*Continuing from Scenario 8:*

1. On the **/results** page:
2. **Verify**: Exactly **3** meal suggestion cards are displayed (SC-006)
3. For each card, **verify**:
   - A **recipe name** is shown
   - A **description** (2–3 sentences) is shown
   - **Numbered cooking steps** (≥ 3 steps) are displayed
4. **Verify**: No extra/partial card is rendered beyond the 3 validated suggestions

---

### Scenario 10 — Meal Request with No Dietary Preferences (User Story 2 Acceptance #3)

1. Add ingredients; leave the dietary preferences field **blank**
2. Submit the meal request
3. **Verify**: Results page shows suggestions without error (no "no preferences" error)

---

### Scenario 11 — Dietary Preferences Character Limit (FR-011, Edge Case)

1. On **/meal-request**, type or paste text that exceeds **500 characters** into the dietary preferences field
2. Click submit
3. **Verify**: Form is re-shown with a validation error: *"Dietary preferences must be 500 characters or fewer."*
4. **Verify**: No API call is made (no results page shown)

---

### Scenario 12 — Navigation from Results (User Story 3 Acceptance #2)

1. After viewing results on **/results**:
2. Click **"Start Over"** (or "My Ingredients")
3. **Verify**: Navigated to **/ingredients** with the session ingredient list still intact
4. Click **"New Search"** (or "Get Suggestions")
5. **Verify**: Navigated to **/meal-request** without having to re-add ingredients

---

### Scenario 13 — AI Service Error Handling (FR-008, User Story 3 Acceptance #3)

1. Temporarily set an **invalid API key** while the server is running:
   - Stop the server; set `ANTHROPIC_API_KEY=invalid-key-test`; restart
2. Add ingredients; submit a meal request
3. **Verify**: **/results** page shows a **user-friendly error message** (not a stack trace)
4. **Verify**: A **"Try Again"** link or button is visible (links back to `/meal-request`)
5. **Verify**: `SC-004` — the error is shown 100% of the time; no raw exception leaked

---

### Scenario 14 — Malformed Suggestion Omission Warning (FR-015, SC-007)

1. Use a test double/fixture for the AI response returning exactly 3 suggestions where 1 item is malformed (e.g., missing `steps`)
2. Submit a normal meal request
3. **Verify**: Only valid suggestions are rendered
4. **Verify**: A visible warning indicates that some suggestions were omitted
5. **Verify**: No server error page is shown

---

### Scenario 15 — Invalid Suggestion Count Handling (FR-014)

1. Use a test double/fixture returning 2 suggestions (or 4 suggestions)
2. Submit a normal meal request
3. **Verify**: Results are treated as error state (not successful suggestions)
4. **Verify**: User sees retry messaging and a clear retry action

---

### Scenario 16 — Mobile Responsiveness (FR-012, SC-005)

1. Open Chrome DevTools (F12) → Toggle device toolbar → Select **iPhone SE** (375 × 667)
2. Navigate through all three screens: **/ingredients**, **/meal-request**, **/results**
3. **Verify**: No horizontal scrollbar appears at 375 px width
4. **Verify**: All key content (ingredient list, form fields, meal cards, buttons) is visible without pinch-zoom
5. Also test at **320 px** width (smallest breakpoint per spec)

---

## Run Automated Tests

```bash
# Run all tests (unit + web layer)
mvn test

# Run a specific test class
mvn test -Dtest=IngredientSessionTest

# Run with verbose output
mvn test -Dsurefire.useFile=false
```

Expected: All tests pass. Key test classes:

| Test Class | What it covers |
|------------|---------------|
| `IngredientSessionTest` | add/remove/dedup/cap logic |
| `IngredientControllerTest` | add/remove form flows; empty-state rendering |
| `MealControllerTest` | form validation; empty-ingredient guard; results rendering |
| `MealSuggestionServiceTest` | prompt building; JSON parse; timeout/error paths (mocked) |

---

## Configuration Reference

| Property | Environment Variable | Default | Override |
|----------|---------------------|---------|---------|
| `anthropic.api-key` | `ANTHROPIC_API_KEY` | *(required)* | — |
| `anthropic.model` | — | `claude-haiku-4-5` | `--anthropic.model=claude-sonnet-4-6` |
| `anthropic.max-tokens` | — | `2048` | `--anthropic.max-tokens=4096` |
| `anthropic.timeout-seconds` | — | `30` | `--anthropic.timeout-seconds=60` |
| `anthropic.meal-count` | — | `3` | Keep fixed at `3` (required by FR-014) |
| `server.port` | — | `8080` | `--server.port=9090` |

---

## Known Limitations (v1)

- Session expires with browser tab close / inactivity timeout (~30 min default); ingredient list is lost
- No database; all data is ephemeral
- Concurrent users share no state (each session is independent) — multi-instance deployment is out of scope
- Claude API costs apply per request; no rate limiting is implemented in v1

## Validation Run Results (Implementation)

- Date: 2026-06-11
- Automated validation executed: `mvn test -DskipITs`
- Result: **PASS** (`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`)
- Covered by automation:
  - Ingredient session deduplication (case-insensitive first-write-wins) and 50-item cap
  - Ingredient and meal controller route guards, form validation, redirects, and results guard
  - Meal service behaviors for exact raw count of 3, malformed suggestion omission with warning, and parse-error fallback
- Manual end-to-end/API-key scenarios were not executed in this environment because no live `ANTHROPIC_API_KEY` was provided.

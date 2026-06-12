# HTTP Routes & View Contracts: Cusina AI

**Feature**: `001-meal-suggestion-webapp`  
**Phase**: 1 — Design  
**Date**: 2025-07-22

---

## Overview

Cusina AI is a server-rendered Spring MVC application. All routes are HTTP endpoints handled by Spring `@Controller` classes; all views are Thymeleaf templates. There is no REST API and no JSON over the wire to the browser.

The application has **three functional screens** plus an error page, connected by form posts and redirects (PRG pattern).

---

## Route Map

```
GET  /                        → redirect to /ingredients
GET  /ingredients             → ingredients.html  (ingredient list management)
POST /ingredients/add         → redirect /ingredients  (add one ingredient)
POST /ingredients/remove      → redirect /ingredients  (remove one ingredient)
GET  /meal-request            → meal-request.html  (dietary prefs + submit)
POST /meal-request/suggest    → [AI call] → redirect /results or /meal-request (error)
GET  /results                 → results.html  (display meal suggestions)
GET  /error                   → error.html  (Spring Boot error mapping)
```

---

## Route Contracts

### `GET /`

**Controller**: `IngredientController`  
**Behaviour**: Redirects to `/ingredients`.  
**Response**: `302 Found` → `/ingredients`

---

### `GET /ingredients`

**Controller**: `IngredientController.showIngredients()`  
**Purpose**: Display current ingredient list; provide add form.

**Model attributes**:

| Attribute | Type | Notes |
|-----------|------|-------|
| `ingredients` | `List<Ingredient>` | Current session ingredient list (may be empty) |
| `isFull` | `boolean` | `true` when 50 ingredients reached; disables add form |
| `addForm` | `IngredientForm` | Form-backing object for the add input field |
| `successMessage` | `String` (optional) | Flash attribute: shown after successful add |
| `errorMessage` | `String` (optional) | Flash attribute: shown after duplicate or cap error |

**Template**: `templates/ingredients.html`  
**Response**: `200 OK` with rendered HTML

---

### `POST /ingredients/add`

**Controller**: `IngredientController.addIngredient()`  
**Purpose**: Add one ingredient to the session list.

**Request body** (form-encoded):

| Field | Type | Validation |
|-------|------|------------|
| `name` | `String` | Required; 1–100 characters after trimming |

**Behaviour**:
1. Validate `name` is not blank; if blank → redirect with `errorMessage="Ingredient name cannot be empty."`
2. Call `IngredientSession.addIngredient(name)`
3. If `false` (duplicate) → redirect with `errorMessage="'<name>' is already in your list."`
4. If `false` (cap reached) → redirect with `errorMessage="You have reached the maximum of 50 ingredients."`
5. If `true` → redirect with `successMessage="'<name>' added."`

**Response**: `302 Found` → `/ingredients`

---

### `POST /ingredients/remove`

**Controller**: `IngredientController.removeIngredient()`  
**Purpose**: Remove one ingredient from the session list.

**Request body** (form-encoded):

| Field | Type | Validation |
|-------|------|------------|
| `normalizedKey` | `String` | Required; the `normalizedKey` of the ingredient to remove |

**Behaviour**:
1. Call `IngredientSession.removeIngredient(normalizedKey)`
2. Redirect unconditionally (no-op if key not found)

**Response**: `302 Found` → `/ingredients`

---

### `GET /meal-request`

**Controller**: `MealController.showMealRequest()`  
**Purpose**: Display the dietary preferences form and a read-only review of the current ingredient list.

**Guard**: If session ingredient list is empty, redirect to `/ingredients` with flash message `"Please add at least one ingredient before requesting meal suggestions."`

**Model attributes**:

| Attribute | Type | Notes |
|-----------|------|-------|
| `ingredients` | `List<Ingredient>` | Read-only display of current session list |
| `mealRequest` | `MealRequest` | Form-backing object for `dietaryPreferences` field |
| `charLimit` | `int` | `500` — passed for client-side character counter display |

**Template**: `templates/meal-request.html`  
**Response**: `200 OK` with rendered HTML

---

### `POST /meal-request/suggest`

**Controller**: `MealController.suggestMeals()`  
**Purpose**: Validate the form, call the Claude API asynchronously, store the result in the session, and redirect.

**Request body** (form-encoded):

| Field | Type | Validation |
|-------|------|------------|
| `dietaryPreferences` | `String` | Optional; max 500 characters |

**Behaviour**:
1. Re-validate ingredient list not empty (defense-in-depth; also enforced at `GET /meal-request`)
2. Validate `dietaryPreferences` ≤ 500 chars; if invalid → return `meal-request.html` with `BindingResult` errors
3. Invoke `MealSuggestionService.suggest(ingredientNames, dietaryPreferences)` — returns `CompletableFuture<MealResponse>`
4. Block with `future.get(30, TimeUnit.SECONDS)`; on `TimeoutException` produce `MealResponse(error="The AI took too long to respond. Please try again.")`
5. Validate raw AI payload count is exactly `3`; if fewer/more, produce `MealResponse(error="We couldn't validate the AI response. Please retry.")`
6. Drop malformed suggestions (missing name/description/steps), keep valid ones, and set warning when any are omitted
7. On `ExecutionException` (API error) produce `MealResponse(error="Unable to reach the AI service. Please try again.")`
8. Store `MealResponse` as flash attribute `mealResponse`
9. Redirect to `/results`

**Response**: `302 Found` → `/results` (or `200 OK` back to `meal-request.html` on validation error)

---

### `GET /results`

**Controller**: `MealController.showResults()`  
**Purpose**: Display meal suggestions or error message.

**Model attributes** (from flash or redirect attributes):

| Attribute | Type | Notes |
|-----------|------|-------|
| `mealResponse` | `MealResponse` | Contains `meals` list and optional `warning`, or `error` |

**Guard**: If `mealResponse` is null (direct navigation without a prior submit), redirect to `/meal-request`.

**Template**: `templates/results.html`  
**Response**: `200 OK` with rendered HTML

**View logic**:
- If `mealResponse.hasError()` → render error block with `mealResponse.error` and "Try Again" link → `/meal-request`
- If `mealResponse.hasWarning()` → render warning banner (`"Some suggestions were omitted."`)
- If `mealResponse.hasMeals()` → render meal cards (one per `MealSuggestion`)
- "Start Over" / "New Search" navigation → `/ingredients`

---

### `GET /error`

**Controller**: Handled by Spring Boot's `BasicErrorController` (default)  
**Purpose**: User-friendly error page for unhandled exceptions (5xx) and not-found (404).

**Template**: `templates/error.html`  
**Response**: `4xx` / `5xx` with rendered HTML

**Model attributes** (Spring Boot error attributes):

| Attribute | Notes |
|-----------|-------|
| `status` | HTTP status code |
| `message` | Short error description (sanitized, no stack trace) |

---

## Thymeleaf View Contracts

### `fragments/layout.html`

Shared page shell. All screens use `th:replace="~{fragments/layout :: page(...)}` or equivalent fragment composition.

| Fragment element | Content |
|-----------------|---------|
| `<head>` | Meta charset/viewport; `<link>` to `/css/style.css`; page title (parameterized) |
| `<nav>` | App name "Cusina AI"; links to "My Ingredients" (`/ingredients`) and "Get Suggestions" (`/meal-request`) |
| `<footer>` | Minimal attribution / version |

---

### `templates/ingredients.html`

| UI Element | Thymeleaf expression | Notes |
|------------|---------------------|-------|
| Page title | `"My Ingredients — Cusina AI"` | |
| Add form | `th:action="@{/ingredients/add}"` `th:object="${addForm}"` | POST; `name` field with `th:field="*{name}"` |
| Add button disabled | `th:disabled="${isFull}"` | Disabled at 50 items |
| Empty state message | `th:if="${#lists.isEmpty(ingredients)}"` | "Add ingredients from your fridge or pantry." |
| Ingredient list | `th:each="ingredient : ${ingredients}"` | Renders `ingredient.displayName` |
| Remove button | `th:action="@{/ingredients/remove}"` form with hidden `normalizedKey` field | POST for each ingredient |
| Success flash | `th:if="${successMessage}"` | Green alert |
| Error flash | `th:if="${errorMessage}"` | Red/amber alert |
| Item count badge | `${#lists.size(ingredients)} / 50` | Shown in header or near list |

---

### `templates/meal-request.html`

| UI Element | Thymeleaf expression | Notes |
|------------|---------------------|-------|
| Page title | `"Get Meal Suggestions — Cusina AI"` | |
| Read-only ingredient review | `th:each="ingredient : ${ingredients}"` | Non-editable list; "Edit" link → `/ingredients` |
| Dietary prefs textarea | `th:object="${mealRequest}"` `th:field="*{dietaryPreferences}"` | `maxlength="500"` |
| Character counter | Static `<span>` target; JS optional or CSS counter trick | Shows remaining characters |
| Validation error | `th:errors="*{dietaryPreferences}"` | Field-level error display |
| Submit button | `th:action="@{/meal-request/suggest}"` | POST |

---

### `templates/results.html`

| UI Element | Thymeleaf expression | Notes |
|------------|---------------------|-------|
| Page title | `"Your Meal Suggestions — Cusina AI"` | |
| Error block | `th:if="${mealResponse.hasError()}"` | Shows `mealResponse.error` + "Try Again" → `/meal-request` |
| Warning block | `th:if="${mealResponse.hasWarning()}"` | Shows omission warning when malformed suggestions were filtered |
| Meal cards container | `th:unless="${mealResponse.hasError()}"` | Grid/flex layout |
| Per-meal card | `th:each="meal : ${mealResponse.meals}"` | |
| Recipe name | `th:text="${meal.name}"` | Bold heading |
| Description | `th:text="${meal.description}"` | Paragraph |
| Steps list | `th:each="step : ${meal.steps}"` | Numbered `<ol>` |
| "Start Over" link | `th:href="@{/ingredients}"` | Resets to ingredient screen (session persists until user clears) |
| "New Search" link | `th:href="@{/meal-request}"` | Goes back to preferences form with same ingredients |

---

### `templates/error.html`

| UI Element | Thymeleaf expression | Notes |
|------------|---------------------|-------|
| Status code | `th:text="${status}"` | e.g. "404", "500" |
| Error description | `th:text="${message}"` | Human-readable; no stack trace |
| Home link | `th:href="@{/}"` | Returns user to ingredient screen |

---

## Form Object Contracts

### `IngredientForm` (add ingredient form binding)

```java
public class IngredientForm {
    @NotBlank(message = "Ingredient name cannot be empty.")
    @Size(max = 100, message = "Ingredient name must be 100 characters or fewer.")
    private String name;
    // getter/setter
}
```

### `MealRequest` (meal request form binding)

```java
public class MealRequest {
    @Size(max = 500, message = "Dietary preferences must be 500 characters or fewer.")
    private String dietaryPreferences;
    // getter/setter
}
```

---

## Security Notes

- The `ANTHROPIC_API_KEY` is read server-side only; it never appears in HTML, JavaScript, or HTTP responses to the browser
- All form inputs are trimmed and validated server-side; Thymeleaf auto-escapes output to prevent XSS
- No CSRF token configuration required for simple session-scoped apps (Spring Boot auto-configures CSRF protection; `th:action` auto-injects `_csrf` hidden field when Spring Security is on the classpath — add `spring-boot-starter-security` if needed)

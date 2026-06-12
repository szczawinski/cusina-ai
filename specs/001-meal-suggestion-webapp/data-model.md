# Data Model: Cusina AI

**Feature**: `001-meal-suggestion-webapp`  
**Phase**: 1 — Design  
**Date**: 2025-07-22

---

## Overview

All state is session-scoped; there is no persistent database. Entities exist either as in-memory session beans or as transient request/response objects assembled per HTTP request.

```
HTTP Session (per browser tab / session cookie)
└── IngredientSession (@SessionScope bean)
    └── List<Ingredient>  ── max 50, deduplicated (case-insensitive)

Per-request lifecycle
├── MealRequest           ── assembled from session + form input at submit time
└── MealResponse          ── returned from Claude API; passed to results view
    └── List<MealSuggestion> (valid suggestions only, 0..3)
```

---

## Entity Definitions

### 1. `Ingredient`

**Package**: `com.cusina.ai.model`  
**Scope**: Held inside `IngredientSession` (session-scoped); not persisted.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `displayName` | `String` | Not blank; max 100 chars | User's original casing (e.g. "Cherry Tomatoes") |
| `normalizedKey` | `String` | Derived; lowercase trimmed | Used for deduplication comparison |

**Validation rules**:
- `displayName` must not be blank after trimming
- `displayName` max 100 characters
- On add: compute `normalizedKey = displayName.trim().toLowerCase()`; reject if `normalizedKey` already present in session list
- Deduplication is case-insensitive: "Chicken" and "chicken" are the same ingredient

**State transitions**: Simple add/remove from the session list. No lifecycle states on the ingredient itself.

**Java representation**:
```java
public record Ingredient(String displayName) {
    public String normalizedKey() {
        return displayName.trim().toLowerCase();
    }
}
```

---

### 2. `MealRequest`

**Package**: `com.cusina.ai.model`  
**Scope**: Transient — created at form submission; not stored in session.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `ingredients` | `List<String>` | Non-empty; populated from session | Ordered list of ingredient display names |
| `dietaryPreferences` | `String` | Optional; max 500 chars | Free text; may be null or blank |

**Validation rules**:
- `ingredients` must contain at least 1 item (FR-005): if empty, redirect back to ingredient screen with validation message
- `dietaryPreferences` max 500 characters; excess is rejected with a field-level error (FR-011)

**Java representation**:
```java
public class MealRequest {
    private List<String> ingredients;

    @Size(max = 500, message = "Dietary preferences must be 500 characters or fewer.")
    private String dietaryPreferences;

    // standard getters/setters for Thymeleaf binding
}
```

---

### 3. `MealSuggestion`

**Package**: `com.cusina.ai.model`  
**Scope**: Transient — populated from Claude API JSON response; passed to results view.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `name` | `String` | Not blank | Recipe name shown to user |
| `description` | `String` | Not blank | Brief dish description |
| `steps` | `List<String>` | Not null; min 1 non-blank item | Ordered cooking instructions |

**Validation rules** (applied after JSON deserialization):
- A suggestion is **malformed** if `name` is blank, `description` is blank, `steps` is null/empty, or any step is blank
- Malformed suggestions are dropped from the rendered result set (FR-015)
- Unknown JSON fields are ignored (`@JsonIgnoreProperties(ignoreUnknown = true)`)

**Java representation**:
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class MealSuggestion {
    private String name;
    private String description;
    private List<String> steps;

    // getters/setters
}
```

---

### 4. `MealResponse`

**Package**: `com.cusina.ai.model`  
**Scope**: Transient — wrapper for the full Claude API response; passed as model to results view.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `meals` | `List<MealSuggestion>` | Not null; 0..3 valid items | Displayable suggestions after malformed filtering |
| `omittedMalformedCount` | `int` | 0..3 | Count of malformed suggestions removed from display |
| `warning` | `String` | Nullable | Set when `omittedMalformedCount > 0` |
| `error` | `String` | Nullable | Populated on API/timeout/invalid-count failures |

**Validation rules**:
- Raw AI response must contain **exactly 3** suggestions; fewer/more is invalid and mapped to error state with retry messaging (FR-014)
- From those 3 suggestions, malformed entries are removed; valid entries are still rendered with warning (FR-015)
- If `error` is non-null, the results view renders error + retry instead of meal cards

**Java representation**:
```java
public class MealResponse {
    private List<MealSuggestion> meals = new ArrayList<>();
    private int omittedMalformedCount;
    private String warning;
    private String error;

    public boolean hasError() { return error != null; }
    public boolean hasMeals() { return meals != null && !meals.isEmpty(); }
    public boolean hasWarning() { return warning != null && !warning.isBlank(); }
}
```

---

### 5. `IngredientSession` (Session State Bean)

**Package**: `com.cusina.ai.session`  
**Scope**: `@SessionScope` Spring bean — one instance per HTTP session.

| Field | Type | Notes |
|-------|------|-------|
| `ingredients` | `LinkedHashSet<Ingredient>` | Insertion-ordered; deduplicated by `normalizedKey` |

**Behavior contract**:

| Method | Signature | Pre-condition | Post-condition |
|--------|-----------|---------------|----------------|
| `addIngredient` | `boolean addIngredient(String name)` | `name` not blank; list size < 50 | Ingredient added if not duplicate; returns `true` if added, `false` if duplicate or cap reached |
| `removeIngredient` | `void removeIngredient(String normalizedKey)` | — | Removes ingredient matching `normalizedKey`; no-op if not found |
| `getIngredients` | `List<Ingredient> getIngredients()` | — | Returns unmodifiable ordered list |
| `getIngredientNames` | `List<String> getIngredientNames()` | — | Returns `displayName` strings for prompt building |
| `isEmpty` | `boolean isEmpty()` | — | Returns `true` when list has no items |
| `isFull` | `boolean isFull()` | — | Returns `true` when list has 50 items |
| `clear` | `void clear()` | — | Removes all ingredients from session |

**Java representation**:
```java
@Component
@SessionScope
public class IngredientSession implements Serializable {

    private static final int MAX_INGREDIENTS = 50;

    // LinkedHashSet stores Ingredient records; uniqueness by normalizedKey
    private final LinkedHashSet<Ingredient> ingredients = new LinkedHashSet<>();

    public boolean addIngredient(String displayName) {
        if (ingredients.size() >= MAX_INGREDIENTS) return false;
        Ingredient candidate = new Ingredient(displayName.trim());
        // Reject duplicate by normalizedKey comparison
        boolean alreadyPresent = ingredients.stream()
            .anyMatch(i -> i.normalizedKey().equals(candidate.normalizedKey()));
        if (alreadyPresent) return false;
        return ingredients.add(candidate);
    }

    public void removeIngredient(String normalizedKey) {
        ingredients.removeIf(i -> i.normalizedKey().equals(normalizedKey));
    }

    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(new ArrayList<>(ingredients));
    }

    public List<String> getIngredientNames() {
        return getIngredients().stream().map(Ingredient::displayName).toList();
    }

    public boolean isEmpty() { return ingredients.isEmpty(); }
    public boolean isFull() { return ingredients.size() >= MAX_INGREDIENTS; }
    public void clear() { ingredients.clear(); }
}
```

---

## Entity Relationships

```
IngredientSession (1) ──contains──▶ (0..50) Ingredient
     │
     │ at submit time
     ▼
MealRequest ──► includes ingredient names from session
                includes optional dietaryPreferences from form
     │
     │ sent to Claude API
     ▼
MealResponse ──contains──▶ (0..3) MealSuggestion (valid only)
            └── tracks omittedMalformedCount + warning
```

---

## State Transitions

### Ingredient List (session)

```
[Empty]
  │ addIngredient() ──── success
  ▼
[1..49 items]
  │ addIngredient()
  ├──► duplicate?  → reject (no change)
  ├──► size == 50? → reject with "list full" message
  └──► otherwise   → add ──▶ [1..50 items]
  │
  │ removeIngredient() → [0..49 items] or [Empty]
```

### Meal Request Flow

```
[Ingredient list empty] ──► submit ──► redirect back with validation error

[Ingredient list has items] ──► submit ──► Claude API call (@Async)
  ├──► raw count != 3 ──► MealResponse(error="Expected exactly 3 suggestions. Please retry.")
  ├──► raw count == 3 + malformed entries present
  │      └──► MealResponse(meals=[valid only], omittedMalformedCount>0, warning="Some suggestions were omitted.")
  ├──► raw count == 3 + all valid ──► MealResponse(meals=[3 items])
  ├──► timeout  ──► MealResponse(error="timeout") ──► results.html (error state)
  └──► API error ──► MealResponse(error="...")    ──► results.html (error state)
```

---

## Configuration Properties

| Property | Default | Notes |
|----------|---------|-------|
| `anthropic.api-key` | *(required)* | Loaded from `ANTHROPIC_API_KEY` env var via `${ANTHROPIC_API_KEY}` |
| `anthropic.model` | `claude-haiku-4-5` | Swap to `claude-sonnet-4-6` for richer output |
| `anthropic.max-tokens` | `2048` | Sufficient for 3 meal suggestions with steps |
| `anthropic.timeout-seconds` | `30` | Hard timeout on the AI call `CompletableFuture.get()` |
| `anthropic.meal-count` | `3 (fixed)` | Must remain 3 to satisfy FR-014; do not override per request |
| `server.port` | `8080` | Default Spring Boot port |

# Data Model: Cusina AI

**Feature**: `001-meal-suggestion-webapp`  
**Phase**: 1 — Design  
**Date**: 2026-06-13

---

## Overview

Model jest session-only (bez bazy) i formalizuje nowe doprecyzowania:
1. nowa sesja startuje z 10 losowymi unikalnymi składnikami,
2. żądanie ma 2 opcjonalne comboboksy o zamkniętych domenach,
3. poprawna sugestia może używać podzbioru składników wejściowych.

---

## Entity Definitions

### 1. `Ingredient`

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `displayName` | `String` | not blank, max 100 | oryginalna forma dla UI |
| `normalizedKey` | `String` (derived) | lowercase+trim | klucz deduplikacji case-insensitive |
| `source` | enum | `PRELOADED` or `USER_ADDED` | źródło wpisu |

---

### 2. `IngredientSession`

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `ingredients` | `LinkedHashSet<Ingredient>` | size 0..50 | zachowana kolejność |
| `initialized` | `boolean` | default `false` | czy preload został wykonany |

**Rules**:
- `initializeIfNeeded()` losuje dokładnie 10 unikalnych składników z `IngredientPool`.
- Duplikat przy add jest ignorowany (first-write-wins).
- Limit sesji: max 50 składników.

---

### 3. `IngredientPool`

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `items` | `List<String>` | curated, unique normalized values | baza do losowania startowych 10 |
| `minPoolSize` | `int` | >= 10 | gwarancja losowania 10 unikalnych |

**Behavior**:
- `drawUnique(count=10)` zwraca 10 unikalnych nazw.
- Jeśli wykryte duplikaty po normalizacji, losowanie kontynuuje do pełnej puli 10.

---

### 4. `MealRequest`

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `ingredients` | `List<String>` | min 1, max 50 | z `IngredientSession` |
| `dietaryPreferences` | `String` | optional, max 500 | wolny tekst |
| `dishType` | enum? | `ŚNIADANIA|LUNCHE|ZUPY|SAŁATKI|MAKARONY|DANIA GŁÓWNE|PODWIECZORKI|NAPOJE|KOLACJE` | odpowiada comboboxowi 1 |
| `dietType` | enum? | `FIT|WEGAŃSKIE|WEGETARIAŃSKIE|BEZGLUTENOWE|NA PATRZE|KUCHNIA AZJATYCKA|KUCHNIA ŚRÓDZIEMNOMORSKA` | odpowiada comboboxowi 2 |
| `locale` | `String` | fixed `pl-PL` | wymuszenie kontekstu językowego |

**Validation**:
- Wartości spoza enum dla `dishType`/`dietType` -> błąd walidacji, brak wywołania AI.

---

### 5. `MealSuggestion`

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `name` | `String` | not blank | nazwa przepisu |
| `description` | `String` | not blank | krótki opis |
| `steps` | `List<String>` | min 1, non-blank items | kroki przygotowania |
| `usedIngredients` | `List<String>` | optional subset | składniki użyte w sugestii |

**Validity rule**:
- Sugestia jest poprawna, gdy `usedIngredients` to dowolny niepusty podzbiór `MealRequest.ingredients` (nie musi obejmować całości listy).

---

### 6. `MealResponse`

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `rawCount` | `int` | must equal 3 for success path | liczba rekordów przed filtrowaniem |
| `validSuggestions` | `List<MealSuggestion>` | 0..3 | tylko rekordy poprawne |
| `omittedMalformedCount` | `int` | 0..3 | ile odrzucono |
| `warningPl` | `String?` | Polish only | np. pominięto część wyników |
| `errorPl` | `String?` | Polish only | np. retry dla rawCount != 3 |

---

## Relationships

```text
IngredientPool --drawUnique(10)--> IngredientSession(initialized)
IngredientSession --submit--> MealRequest
MealRequest --Claude API--> MealResponse(rawCount must be 3)
MealResponse --contains--> MealSuggestion(0..3 valid)
MealSuggestion.usedIngredients ⊆ MealRequest.ingredients (non-empty subset allowed)
```

---

## State Transitions

```text
New session
  -> initializeIfNeeded()
  -> IngredientSession has exactly 10 PRELOADED unique items

Meal request submit
  -> invalid enum value in combobox => validation error, stay on form, no AI call
  -> rawCount != 3 => errorPl + retry path
  -> rawCount == 3 && malformed > 0 => partial success + warningPl
  -> rawCount == 3 && malformed == 0 => success
```

---

## Validation Matrix (business-critical)

| Rule | Source |
|---|---|
| Exactly 10 random startup ingredients | FR-004 |
| Closed lists for 2 comboboxes | FR-006, FR-007, FR-026 |
| Subset usage accepted in suggestions | FR-012 |
| Exact-3 response + malformed omission | FR-019, FR-020 |
| Ingredient dedup + max 50 | FR-015, FR-021 |

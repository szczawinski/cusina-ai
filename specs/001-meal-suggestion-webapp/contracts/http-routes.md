# HTTP Routes & View Contracts: Cusina AI

**Feature**: `001-meal-suggestion-webapp`  
**Phase**: 1 — Design  
**Date**: 2026-06-13

---

## Overview

Aplikacja jest server-rendered (Spring MVC + Thymeleaf). Kontrakt wymaga:
- pełnego UI po polsku,
- premium minimalistycznego layoutu,
- poprawnego renderingu na iPhone Safari 320–430 px,
- zgodności operacyjnej z Cloud Run.

---

## Route Map

```text
GET  /                         -> 302 /ingredients
GET  /ingredients              -> ingredients.html
POST /ingredients/add          -> 302 /ingredients
POST /ingredients/remove       -> 302 /ingredients
GET  /meal-request             -> meal-request.html
POST /meal-request/suggest     -> 302 /results (lub 200 z błędami walidacji)
GET  /results                  -> results.html
GET  /error                    -> error.html

# operacyjne (Cloud Run)
GET  /actuator/health          -> 200
GET  /actuator/health/readiness -> 200
```

---

## Route Contracts

### `GET /ingredients`
- Jeśli sesja nie była inicjalizowana, wykonaj preload dokładnie 10 losowych unikalnych składników.
- Model zawiera: `ingredients`, `addForm`, komunikaty PL.
- Widok pokazuje źródło listy startowej (preload) i pozwala edytować listę.

### `POST /ingredients/add`
- Input: `name` (trim, 1..100).
- Reguły: deduplikacja case-insensitive, limit sesji 50.
- Duplicate nie powoduje błędu krytycznego: wpis ignorowany + komunikat informacyjny PL.
- PRG redirect do `/ingredients`.

### `POST /ingredients/remove`
- Input: `normalizedKey`.
- Operacja idempotentna.
- PRG redirect do `/ingredients`.

### `GET /meal-request`
- Guard: pusta lista składników -> redirect `/ingredients` + komunikat PL.
- Formularz zawiera:
  - `dietaryPreferences` (optional, max 500),
  - `dishType` combobox: `śniadanie|obiad|deser`,
  - `dietType` combobox: `lekka|śródziemnomorska|wegetariańska`.

### `POST /meal-request/suggest`
- Walidacje wejścia (server-side):
  1) lista składników niepusta,
  2) `dietaryPreferences` <= 500,
  3) `dishType` w zamkniętej liście,
  4) `dietType` w zamkniętej liście.
- Naruszenie 3/4 => błąd walidacji PL + brak wywołania AI.
  - Przykładowe komunikaty: „Nieprawidłowy typ dania...” / „Nieprawidłowy typ diety...”.
- Po odpowiedzi AI:
  - `rawCount != 3` => ścieżka błędu/retry,
  - `rawCount == 3` => filtr malformed i przekazanie warningu, jeśli coś odrzucono.
- Redirect do `/results` (dla poprawnie obsłużonego wyniku).

### `GET /results`
- Guard: brak `mealResponse` -> redirect `/meal-request`.
- Renderuje:
  - `errorPl` (gdy błąd),
  - `warningPl` (gdy pominięto malformed),
  - `validSuggestions`.
- Kontrakt biznesowy: sugestia jest akceptowalna, gdy używa **niepustego** podzbioru składników wejściowych.

---

## Request/Response Shape (logical)

### Meal request (logical payload do serwisu AI)

```json
{
  "ingredients": ["pomidor", "jajko", "ser"],
  "dietaryPreferences": "bez orzechów",
  "dishType": "obiad",
  "dietType": "wegetariańska",
  "locale": "pl-PL"
}
```

### Meal response (logical shape po walidacji)

```json
{
  "rawCount": 3,
  "validSuggestions": [
    {
      "name": "Jajecznica z pomidorami",
      "description": "Szybkie danie na ciepło",
      "steps": ["..."],
      "usedIngredients": ["jajko", "pomidor"]
    }
  ],
  "omittedMalformedCount": 0,
  "warningPl": null,
  "errorPl": null
}
```

`usedIngredients` musi być niepustym podzbiorem `ingredients`.

---

## UX/Language Constraints

- Wszystkie komunikaty i etykiety są po polsku.
- Dwa comboboksy muszą być selectami z zamkniętymi opcjami (bez wartości niestandardowych).
- Layout musi pozostać używalny na 320–430 px bez poziomego scrolla.

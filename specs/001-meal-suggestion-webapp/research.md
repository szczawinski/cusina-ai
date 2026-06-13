# Research: Cusina AI — Phase 0 Findings

**Feature**: `001-meal-suggestion-webapp`  
**Date**: 2026-06-13  
**Status**: Complete

---

## 1) Start sesji: 10 losowych składników

**Decision**: Przy pierwszym wejściu do nowej sesji system losuje dokładnie 10 unikalnych składników z kuratorowanej puli i zapisuje je w `IngredientSession`.

**Rationale**:
- Bezpośrednie pokrycie FR-004 i scenariusza „10 na starcie”.
- Spójny onboarding: użytkownik może od razu testować przepływ sugestii.
- Losowanie ze zbiorem + deduplikacja daje przewidywalny efekt biznesowy (zawsze 10).

**Alternatives considered**:
- Stała lista 10 pozycji — odrzucona (brak losowości wymaganej doprecyzowaniem).
- Mniej niż 10 przy duplikatach — odrzucone (sprzeczne z wymaganiem „dokładnie 10”).

---

## 2) Dwa comboboksy z zamkniętymi listami wartości

**Decision**: Formularz `/meal-request` udostępnia dokładnie 2 opcjonalne pola typu select:
- `dishType`: `śniadanie | obiad | deser`
- `dietType`: `lekka | śródziemnomorska | wegetariańska`

Walidacja backendowa odrzuca wartości spoza list i blokuje wywołanie AI.

**Rationale**:
- Pokrycie FR-006, FR-007, FR-026.
- Ochrona przed manipulacją payloadem poza UI.
- Lepsza spójność wyników i prostsza walidacja kontraktu.

**Alternatives considered**:
- Wolny tekst dla typu dania/diety — odrzucony (sprzeczny z zamkniętą domeną).
- Walidacja tylko po stronie UI — odrzucona (niewystarczające bezpieczeństwo).

---

## 3) Sugestie mogą wykorzystywać podzbiór składników

**Decision**: Walidator odpowiedzi traktuje sugestię jako poprawną, gdy wykorzystuje dowolny niepusty podzbiór przekazanych składników (pełne zużycie listy nie jest wymagane).

**Rationale**:
- Pokrycie FR-012 i scenariusza akceptacyjnego dla podzbioru.
- Lepsza użyteczność: model nie jest sztucznie ograniczany do „użyj wszystkiego”.

**Alternatives considered**:
- Wymóg użycia wszystkich składników — odrzucony (sprzeczne z doprecyzowaniem).
- Brak jakiejkolwiek kontroli powiązania ze składnikami — odrzucony (za luźny kontrakt jakości).

---

## 4) Polityka odpowiedzi AI: exact-3 + malformed

**Decision**:
1. Surowa lista musi mieć dokładnie 3 sugestie.
2. Sugestie malformed są odrzucane punktowo.
3. Użytkownik dostaje ostrzeżenie PL przy częściowych wynikach.

**Rationale**:
- Spójne z FR-019 i FR-020.
- Zachowuje użyteczne wyniki bez łamania reguł jakości.

**Alternatives considered**:
- Akceptacja dowolnej liczby sugestii — odrzucona.
- Odrzucanie całej odpowiedzi po jednym błędzie — odrzucone.

---

## 5) Podsumowanie zamknięcia niejasności

| Obszar | Status |
|---|---|
| 10 losowych składników na starcie | Rozstrzygnięte |
| 2 comboboksy z zamkniętymi listami | Rozstrzygnięte |
| Podzbiór składników jako poprawna sugestia | Rozstrzygnięte |
| Exact-3 i obsługa malformed | Rozstrzygnięte |

Brak otwartych pozycji NEEDS CLARIFICATION.

# Quickstart & Validation Guide: Cusina AI

**Feature**: `001-meal-suggestion-webapp`  
**Phase**: 1 — Design  
**Date**: 2026-06-13

---

## Purpose

Szybki przewodnik uruchomienia i walidacji po nowych doprecyzowaniach:
- 10 losowych składników na starcie sesji,
- 2 comboboksy o zamkniętych listach,
- poprawność sugestii używających podzbioru składników.

Powiązane artefakty: [data-model.md](./data-model.md), [contracts/http-routes.md](./contracts/http-routes.md).

---

## Prerequisites

- Java 21
- Maven 3.8+
- Ustawione `ANTHROPIC_API_KEY`

### Konfiguracja persystencji składników (Firestore + fallback lokalny)

- Domyślnie lokalnie używany jest fallback plikowy (`ingredients.persistence.file=data/ingredients.json`).
- Firestore włączysz przez `FIRESTORE_ENABLED=true`.
- Credentials są wykrywane automatycznie przez ADC:
  1. `GOOGLE_APPLICATION_CREDENTIALS` (lokalny plik service account),
  2. metadata service na Cloud Run (bez pliku klucza).
- Opcjonalnie ustaw:
  - `GOOGLE_CLOUD_PROJECT`,
  - `FIRESTORE_COLLECTION` (domyślnie `ingredientSessions`).

---

## Run locally

```bash
mvn clean test
```

```bash
# PowerShell
$env:ANTHROPIC_API_KEY="<key>"
mvn spring-boot:run
```

```bash
# PowerShell: lokalnie z Firestore (mvn spring-boot:run)
$env:ANTHROPIC_API_KEY="<key>"
$env:FIRESTORE_ENABLED="true"
$env:GOOGLE_CLOUD_PROJECT="cusina-ai"
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\\path\\to\\service-account.json"
mvn spring-boot:run
```

```bash
# PowerShell: lokalnie z Firestore (java -jar)
$env:ANTHROPIC_API_KEY="<key>"
$env:FIRESTORE_ENABLED="true"
$env:GOOGLE_CLOUD_PROJECT="cusina-ai"
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\\path\\to\\service-account.json"
java -jar target/cusina-ai-0.0.1-SNAPSHOT.jar
```

```bash
# Docker lokalnie z Firestore (Windows PowerShell)
# 1) zbuduj obraz
cd C:\Users\e-prcw\workspace\cusina-ai
docker build -t cusina-ai:local .

# 2) uruchom z montowaniem klucza service account
$env:GOOGLE_CREDENTIALS_HOST="C:\\path\\to\\service-account.json"
docker run --rm -p 8080:8080 \
  -e ANTHROPIC_API_KEY="<key>" \
  -e FIRESTORE_ENABLED="true" \
  -e GOOGLE_CLOUD_PROJECT="cusina-ai" \
  -e GOOGLE_APPLICATION_CREDENTIALS="/secrets/gcp-sa.json" \
  -v "$env:GOOGLE_CREDENTIALS_HOST:/secrets/gcp-sa.json:ro" \
  cusina-ai:local
```

Aplikacja: `http://localhost:8080/ingredients`

> Uwaga kosztowa: Firestore ma darmowy tier. Przy tej aplikacji (niskie R/W) zwykle wystarcza, ale warto ustawić alert budżetowy w GCP.

---

## Validation Scenarios (E2E)

### 1) Start sesji = dokładnie 10 losowych składników
1. Otwórz nową sesję (incognito lub nowy `JSESSIONID`).
2. Wejdź na `/ingredients`.
3. Oczekuj dokładnie 10 pozycji, bez duplikatów case-insensitive.

### 2) Dwa comboboksy z zamkniętymi wartościami
1. Wejdź na `/meal-request` z niepustą listą składników.
2. Zweryfikuj obecność dokładnie 2 selectów:
   - `śniadania|lunche|zupy|sałatki|makarony|dania główne|podwieczorki|napoje|kolacje`
   - `fit|wegańskie|wegetariańskie|bezglutenowe|na patrze|kuchnia azjatycka|kuchnia śródziemnomorska`
3. Spróbuj wysłać payload z wartością spoza list (np. przez devtools).
4. Oczekuj walidacji PL i braku wywołania AI.

### 3) Podzbiór składników jest poprawny
1. Podaj listę np. `pomidor, jajko, ser, cebula`.
2. Zasymuluj poprawną odpowiedź AI, gdzie sugestia używa tylko `jajko, pomidor`.
3. Oczekuj wyniku jako poprawnego (bez błędu o „braku pełnego użycia” składników).

### 4) Exact-3 + malformed policy
1. Fixture A: `rawCount = 2` lub `4` -> oczekuj błędu i retry.
2. Fixture B: `rawCount = 3`, 1 rekord malformed -> oczekuj wyświetlenia poprawnych + ostrzeżenia PL.

### 5) Polski UI i mobile
1. Sprawdź główne widoki (`/ingredients`, `/meal-request`, `/results`) pod kątem języka PL.
2. Zweryfikuj brak poziomego scrolla dla 320/375/390/414/430 px.

### 6) Matryca viewportów i orientacji (iPhone Safari)

| Widok | 320P | 320L | 375P | 390P | 414P | 430P |
|---|---|---|---|---|---|---|
| `/ingredients` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/meal-request` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/results` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

Legenda: `P` = portrait, `L` = landscape; ✅ = brak poziomego scrolla i klikalne CTA.

### 7) Premium-minimalist visual QA checklist

- [x] Typografia czytelna przy 320 px (nagłówki + treść + komunikaty walidacji)
- [x] Spacing kart i pól formularza bez nakładania elementów
- [x] Widoczne cue głębi (delikatny cień + separacja kart)
- [x] Przycisk główny i przyciski akcji mają touch target >= 44 px
- [x] Błędy walidacji dla comboboxów zawijają się poprawnie na wąskich ekranach

---

## Minimal command checklist

```bash
mvn test
```

Manualnie potwierdzić:
- scenariusz 10 składników startowych,
- walidację 2 comboboxów,
- akceptację podzbioru składników w sugestiach,
- exact-3 i obsługę malformed.

---

## Wyniki walidacji (2026-06-13)

- `mvn -B test` ✅
- 45 testów: `0 failures`, `0 errors`, `0 skipped`
- Pokryte automatycznie: preload 10 składników, walidacja comboboxów, wpływ `dishType`/`dietType` na prompt AI, akceptacja niepustego podzbioru składników, polityka exact-3 + malformed.

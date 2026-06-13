# Feature Specification: Cusina AI — Meal Suggestion Web Application

**Feature Branch**: `001-meal-suggestion-webapp`

**Created**: 2025-07-22

**Status**: Draft

**Input**: User description: "Build a web application called cusina-ai using Java 21, Spring Boot 3, Thymeleaf, and Maven. The app helps users generate meal suggestions based on available ingredients using the Anthropic Claude API."

---

## Clarifications

### Session 2026-06-11

- Q: How should suggestion count be handled on successful AI responses? → A: Exactly 3 suggestions are required; fewer or more is treated as an error with retry messaging.
- Q: When some AI suggestions are malformed, how should results be handled? → A: Drop malformed suggestions, display only valid suggestions, and show a warning that some suggestions were omitted.
- Q: How should duplicate ingredients be handled? → A: Deduplicate case-insensitively, keep the first occurrence, and ignore later repeats.

### Session 2026-06-13

- Q: What language should be used across the product UI and presented suggestion content? → A: Polish only.
- Q: What visual and mobile quality bar is required? → A: Minimalist premium style inspired by apple.com and high-quality iPhone Safari rendering in the 320-430 px range.
- Q: How should combobox value domains be defined for dish type and diet type? → A: Option A — canonical, closed lists; dish type = `śniadania`, `lunche`, `zupy`, `sałatki`, `makarony`, `dania główne`, `podwieczorki`, `napoje`, `kolacje`; diet type = `fit`, `wegańskie`, `wegetariańskie`, `bezglutenowe`, `na patrze`, `kuchnia azjatycka`, `kuchnia śródziemnomorska`; values outside these lists are rejected by validation and the request is not sent.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Manage Ingredient List (Priority: P1)

A home cook opens the application and starts with an automatically prepared list of 10 random fridge-friendly ingredients. They can add ingredients one at a time and remove any that were entered by mistake. The list persists in their session so they can continue editing before submitting.

**Why this priority**: Without an ingredient list there is nothing to send to the AI; this is the foundation of the entire feature set.

**Independent Test**: Can be fully tested by launching the app, confirming 10 prefilled ingredients are shown, adding several ingredients, removing one, and confirming the correct list is displayed.

**Acceptance Scenarios**:

1. **Given** a new user session starts and the user opens the ingredient screen for the first time, **When** the page loads, **Then** exactly 10 unique, common fridge ingredients are already present in the list.
2. **Given** the user is on the ingredient list screen, **When** they type an ingredient name and submit the add form, **Then** the ingredient appears in the list on the same page without a full page reload (or via a clean form-post redirect).
3. **Given** the ingredient list contains at least one item, **When** the user clicks the remove control next to an ingredient, **Then** that ingredient is removed from the list and the updated list is shown immediately.
4. **Given** the user has removed all ingredients from the list, **When** they view the ingredient screen, **Then** a helpful prompt encourages them to add ingredients.
5. **Given** the user has added or removed ingredients, **When** they navigate away and return within the same session, **Then** their current ingredient list state is still present.
6. **Given** an ingredient already exists in the list (case-insensitive match), **When** the user adds the same ingredient again, **Then** the original entry is kept and the duplicate attempt is ignored.
7. **Given** the user is on the ingredient screen, **When** they view labels, placeholders, buttons, helper text, and validation messages, **Then** all visible interface text is in Polish.

---

### User Story 2 — Request Meal Suggestions (Priority: P2)

A user who has an ingredient list navigates to the meal request screen, optionally enters free-text dietary preferences, optionally selects a dish type, and optionally selects a diet type, then submits the request to receive AI-generated meal ideas.

**Why this priority**: Submitting a request to the AI is the core value action of the app; it directly delivers the meal-suggestion outcome users came for.

**Independent Test**: Can be tested independently by pre-populating the session with ingredients, navigating to the meal request screen, selecting optional combobox values, submitting, and verifying the results screen appears with content.

**Acceptance Scenarios**:

1. **Given** the user has at least one ingredient in their list, **When** they navigate to the meal request screen, **Then** their current ingredient list is shown for review.
2. **Given** the meal request screen is displayed, **When** the user enters free-text dietary preferences and submits, **Then** the request is forwarded to the AI with both the ingredient list and the free-text preferences included.
3. **Given** the meal request screen is displayed, **When** the user selects a dish type from the combobox (`śniadania`, `lunche`, `zupy`, `sałatki`, `makarony`, `dania główne`, `podwieczorki`, `napoje`, `kolacje`) and submits, **Then** the selected dish type is included in the request context and influences generated suggestions.
4. **Given** the meal request screen is displayed, **When** the user selects a diet type from the combobox (`fit`, `wegańskie`, `wegetariańskie`, `bezglutenowe`, `na patrze`, `kuchnia azjatycka`, `kuchnia śródziemnomorska`) and submits, **Then** the selected diet type is included in the request context and influences generated suggestions.
5. **Given** the user submits a request with no optional preferences selected or entered, **Then** the system uses the ingredient list alone and returns suggestions without error.
6. **Given** the user attempts to submit with an empty ingredient list, **Then** the system presents a clear validation message directing them to add ingredients first.
7. **Given** the user is on the meal request screen, **When** they read instructions and validation feedback, **Then** all text is presented in Polish and uses natural Polish wording.
8. **Given** the submitted request contains a dish type or diet type value outside the canonical combobox lists (e.g., tampered payload), **Then** submission is rejected with a Polish validation message, the user remains on the meal request screen, and no AI call is made.

---

### User Story 3 — View AI-Generated Meal Suggestions (Priority: P3)

After submitting their request, the user sees a results screen showing the meal suggestions returned by the AI. Each suggestion displays a recipe name, a brief description of the dish, and step-by-step cooking instructions so the user can act on the recommendation immediately.

**Why this priority**: Displaying results is the payoff for the user's effort; however, it depends on Stories 1 and 2 being complete, making it logically third.

**Independent Test**: Can be tested by mocking the AI response with a fixture and verifying the results screen renders the structured output correctly (name, description, steps per meal).

**Acceptance Scenarios**:

1. **Given** the AI returns a valid response, **When** the results screen is rendered, **Then** each suggestion displays a recipe name, a short description, and numbered cooking steps.
2. **Given** the results screen is displayed, **When** the user wants to start over, **Then** a clear navigation control takes them back to the ingredient list screen.
3. **Given** the AI service is unavailable or returns an error, **When** the results screen would be shown, **Then** a user-friendly error message is displayed instead of a stack trace, and the user is offered a way to retry.
4. **Given** the AI returns exactly 3 well-formed suggestions, **Then** all 3 suggestions are displayed in a scannable layout where the user can compare options side by side or in sequence.
5. **Given** the AI returns exactly 3 suggestions but one or more are malformed, **Then** the system drops malformed entries, displays only valid suggestions, and shows a warning that some suggestions were omitted.
6. **Given** the AI returns fewer than 3 or more than 3 suggestions, **Then** the system treats the response as invalid, shows retry messaging, and does not present results as a successful outcome.
7. **Given** a valid suggestion uses only a subset of the available ingredient list, **When** the results are displayed, **Then** the suggestion is treated as valid and shown without warning.
8. **Given** valid suggestions are displayed, **When** the user reads recipe names, descriptions, and cooking steps, **Then** all displayed suggestion content is in Polish.

---

### User Story 4 — Experience Premium Polish UI on iPhone (Priority: P2)

A user opens the app on iPhone Safari and expects a polished, minimalist, premium-feeling interface with generous whitespace, subtle depth, and clear visual hierarchy while all text remains in Polish.

**Why this priority**: The feature must be usable and visually consistent for the primary mobile audience, especially iPhone Safari widths from 320 to 430 px.

**Independent Test**: Can be tested by opening the app on iPhone Safari (or equivalent viewport simulation) at 320, 375, 390, 414, and 430 px widths and verifying style, readability, and usability criteria.

**Acceptance Scenarios**:

1. **Given** the app is viewed on iPhone Safari in the 320-430 px range, **When** the user navigates across ingredient, request, and results screens, **Then** no horizontal scrolling is required and no content is clipped.
2. **Given** the user views any primary screen, **When** they assess visual presentation, **Then** the interface shows a minimalist premium style with large whitespace, restrained visual noise, and subtle gradient/shadow accents.
3. **Given** the user interacts with primary actions (add, remove, submit, retry), **When** controls are rendered on iPhone Safari, **Then** touch targets are clear, readable, and consistently spaced to prevent accidental taps.
4. **Given** the user switches between portrait and landscape orientation on iPhone Safari, **When** the layout reflows, **Then** hierarchy, readability, and action accessibility remain intact.

---

### Edge Cases

- What happens when a new session starts but the random preload source contains duplicates? The system retries selection until 10 unique ingredients are presented.
- What happens when the user adds duplicate ingredients? The system performs case-insensitive deduplication, keeps the first occurrence, and ignores later repeats.
- What happens when the user removes all prefilled ingredients and submits a meal request? Submission is blocked with a clear message requiring at least one ingredient.
- What happens when the AI API key is missing or invalid? The application fails gracefully at startup with a clear configuration error rather than crashing mid-request.
- What happens when the AI response takes longer than expected? The user sees a loading indicator; after a configurable timeout the system shows an error and offers a retry.
- What happens when the ingredient list exceeds a reasonable length? The system accepts up to 50 ingredients per session to keep prompts focused and avoid API token limits.
- What happens when free-text dietary preferences are excessively long? Input is capped at 500 characters with a visible character counter.
- What happens when selected dish type and selected diet type are logically conflicting? The system still submits both choices, and returned suggestions must attempt to satisfy both without system error.
- What happens when submitted dish type or diet type is outside the canonical combobox list (including manipulated client payload)? The system rejects the request, shows a Polish validation message indicating allowed values, and does not call the AI API.
- What happens when ingredient or preference text includes Polish diacritics (ą, ć, ę, ł, ń, ó, ś, ź, ż)? The system preserves characters exactly in input and display.
- What happens when AI output contains non-Polish text? The system treats it as invalid output for this feature, informs the user in Polish, and offers retry.
- What happens when the app is used on iPhone Safari at 320 px width with long text labels? Text wraps without truncating critical actions, and no horizontal scroll appears.
- What happens when subtle gradients/shadows are unavailable or reduced by device/browser settings? The UI remains readable, high-contrast, and fully usable with a clean minimalist fallback.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to add ingredients to a session-scoped ingredient list from a dedicated ingredient management screen.
- **FR-002**: Users MUST be able to remove any ingredient from their current session list.
- **FR-003**: The system MUST display the current ingredient list on both the ingredient screen and the meal request screen so users can confirm what will be submitted.
- **FR-004**: On first load of a new session, the system MUST automatically prefill the ingredient list with exactly 10 unique, common fridge ingredients.
- **FR-005**: The meal request screen MUST provide optional free-text dietary preferences or constraints (e.g., "vegetarian", "no nuts", "low carb").
- **FR-006**: The meal request screen MUST provide an optional dish-type combobox with a closed canonical value list: `śniadania`, `lunche`, `zupy`, `sałatki`, `makarony`, `dania główne`, `podwieczorki`, `napoje`, `kolacje`.
- **FR-007**: The meal request screen MUST provide an optional diet-type combobox with a closed canonical value list: `fit`, `wegańskie`, `wegetariańskie`, `bezglutenowe`, `na patrze`, `kuchnia azjatycka`, `kuchnia śródziemnomorska`.
- **FR-008**: The system MUST prevent submission of a meal request when the ingredient list is empty, displaying a clear validation message.
- **FR-009**: The system MUST forward the ingredient list and all selected/entered optional request inputs (free-text preference, dish type, diet type) to the Anthropic Claude API and retrieve meal suggestions.
- **FR-010**: If a dish type and/or diet type is selected, generated suggestions MUST reflect the selected values.
- **FR-011**: The results screen MUST display each AI-generated suggestion with at minimum: a recipe name, a brief description, and step-by-step cooking instructions.
- **FR-012**: Meal suggestions MUST be considered valid when they use any non-empty subset of the provided ingredient list; using all listed ingredients is not required.
- **FR-013**: The system MUST handle AI API errors gracefully, showing a user-friendly message and a retry option rather than an unhandled error page.
- **FR-014**: The Anthropic Claude API key MUST be supplied via an environment variable; the application MUST refuse to start without it.
- **FR-015**: The ingredient list MUST be limited to 50 items per session to maintain prompt quality and performance.
- **FR-016**: Free-text dietary preference input MUST be capped at 500 characters.
- **FR-017**: The interface MUST be responsive and usable on mobile-sized screens (320 px and above) without horizontal scrolling.
- **FR-018**: All user-facing interface text (navigation, labels, placeholders, buttons, helper text, validation, warnings, and error messages) MUST be presented in Polish.
- **FR-019**: A successful AI response MUST contain exactly 3 suggestion items from the AI; responses with fewer or more than 3 suggestions MUST be handled as an error state with clear retry messaging.
- **FR-020**: If any of the 3 returned suggestion items are malformed, the system MUST drop only malformed items, display the remaining valid suggestions, and show a clear warning that some suggestions were omitted.
- **FR-021**: Ingredient entries MUST be unique by case-insensitive name; when a duplicate is submitted, the system MUST keep the first occurrence and ignore later repeats.
- **FR-022**: Displayed meal suggestion content (recipe name, description, and cooking steps) MUST be in Polish.
- **FR-023**: The visual design MUST follow a minimalist, premium style inspired by apple.com, including generous whitespace, restrained visual noise, and subtle gradient/shadow depth cues.
- **FR-024**: The full user journey (ingredient management, request submission, results, errors) MUST render correctly on iPhone Safari across viewport widths 320-430 px.
- **FR-025**: On iPhone Safari widths 320-430 px, all primary actions MUST remain visible and operable without zooming or horizontal scrolling.
- **FR-026**: If submitted dish type or diet type is outside its canonical list, the system MUST reject the request with a Polish validation message, keep the user on the request screen, and MUST NOT send the request to the Anthropic Claude API.

### Key Entities

- **Ingredient**: A named food item available for suggestion generation. Attributes: name (string), source (`preloaded` or `user_added`). Identity rule: unique by case-insensitive name within a session list; first-entered value is retained. Scope: session-lifetime only; not persisted to a database.
- **MealRequest**: The payload submitted to the AI. Attributes: ingredient list (ordered collection of Ingredient names), dietary preferences (optional free text), dish type (optional predefined value from `śniadania` | `lunche` | `zupy` | `sałatki` | `makarony` | `dania główne` | `podwieczorki` | `napoje` | `kolacje`), diet type (optional predefined value from `fit` | `wegańskie` | `wegetariańskie` | `bezglutenowe` | `na patrze` | `kuchnia azjatycka` | `kuchnia śródziemnomorska`), ingredient usage rule (suggestions may use a subset of provided ingredients). Validation: values outside combobox domains are invalid and block submission.
- **MealSuggestion**: One AI-generated dish returned in the response. Attributes: recipe name, brief description, ordered list of cooking steps, referenced ingredients (zero or more ingredient names from the request list).
- **MealResponse**: The structured collection of MealSuggestion items returned from one AI call, associated with a MealRequest.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On first screen load of a new session, exactly 10 prefilled ingredients are visible within 1 second under normal conditions.
- **SC-002**: Users can add an ingredient and see it reflected in the list in under 1 second under normal network conditions.
- **SC-003**: Users can complete the full flow — review prefilled ingredients, adjust the list, optionally set request preferences, submit, view results — in under 3 minutes on first use.
- **SC-004**: Meal suggestion results are returned and displayed within 15 seconds of submission under typical API response times.
- **SC-005**: The application displays a meaningful error message (not a raw stack trace) 100% of the time when the AI service is unavailable.
- **SC-006**: The results screen is usable on a 375 px wide mobile screen with no horizontal scrolling required and all key content visible without zooming.
- **SC-007**: For successful responses with 3 well-formed suggestions, exactly 3 distinct meal suggestions are displayed, each with a name, description, and at least 3 cooking steps.
- **SC-008**: When a successful 3-item response contains malformed suggestion items, 100% of malformed items are excluded from display, valid items are still shown, and a visible warning indicates that some suggestions were omitted.
- **SC-009**: The application starts and runs without error when a valid API key is supplied, and produces a clear startup failure message when the key is absent.
- **SC-010**: When users attempt to add duplicate ingredients that differ only by case, 100% of duplicate attempts are ignored and the originally added ingredient remains unchanged in the list.
- **SC-011**: In acceptance testing of the primary flow, 100% of audited user-facing UI strings are in Polish.
- **SC-012**: On iPhone Safari at 320, 375, 390, 414, and 430 px widths, 0 critical layout defects are observed (no clipped key content, no horizontal scrolling, no inaccessible primary action).
- **SC-013**: In request-audit tests where dish type and/or diet type is selected, 100% of submitted requests include the selected combobox values and produce suggestions classified by reviewers as matching selected filters in at least 80% of cases.
- **SC-014**: In results QA with varied ingredient list sizes, suggestions that use only a subset of listed ingredients are accepted as valid in 100% of tested cases, provided all other response rules are met.
- **SC-015**: In design QA review against a predefined visual checklist, all primary screens pass required premium-minimalist style criteria (whitespace consistency, restrained palette, subtle depth, clear hierarchy).
- **SC-016**: In validation tests for request submission, 100% of out-of-list dish type or diet type values are rejected before API call, with a Polish validation message shown and zero unauthorized AI requests sent.

---

## Assumptions

- Users have a modern web browser (Chrome, Firefox, Safari, Edge — last 2 major versions); no IE11 support is required.
- No user account system or persistent database is needed; all state lives in the HTTP session within a single browser session.
- On each new session, the initial 10 prefilled ingredients are drawn randomly from a curated set of common fridge products appropriate for Polish users.
- Users may keep, remove, or extend the prefilled list before submitting a meal request.
- The Anthropic Claude API is accessed over HTTPS from the server side; the API key is never exposed to the browser.
- A single deployment environment is assumed (local development or single-server deploy); multi-instance session sharing is out of scope.
- The application will serve one user at a time per session; concurrent-user load testing is out of scope for v1.
- Recipes are presented as returned by the AI; the application does not validate nutritional content or safety of suggestions.
- The app will be packaged as a self-contained runnable JAR; no external web server setup is required.
- Ingredient deduplication is case-insensitive and first-write-wins (e.g., if "Chicken" is added first, later "chicken" is ignored).
- No image assets are required; the UI relies entirely on typography, spacing, and color for visual hierarchy.
- Polish is the default and only supported UI language for this feature version.
- "Style inspired by apple.com" is interpreted as outcome-focused visual direction (minimalist premium feel), not copying branded assets or proprietary design elements.
- Meal suggestions are expected to use relevant available ingredients but are not required to consume all ingredients listed in the request.

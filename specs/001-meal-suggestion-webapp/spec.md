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

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Manage Ingredient List (Priority: P1)

A home cook opens the application and builds their ingredient list by typing in whatever they currently have in their fridge and pantry. They can add ingredients one at a time and remove any that were entered by mistake. The list persists in their session so they can continue adding before submitting.

**Why this priority**: Without an ingredient list there is nothing to send to the AI; this is the foundation of the entire feature set.

**Independent Test**: Can be fully tested by launching the app, navigating to the ingredient screen, adding several ingredients, removing one, and confirming the correct list is displayed — delivers a useful "pantry tracker" experience independently.

**Acceptance Scenarios**:

1. **Given** the user is on the ingredient list screen, **When** they type an ingredient name and submit the add form, **Then** the ingredient appears in the list on the same page without a full page reload (or via a clean form-post redirect).
2. **Given** the ingredient list contains at least one item, **When** the user clicks the remove control next to an ingredient, **Then** that ingredient is removed from the list and the updated list is shown immediately.
3. **Given** the ingredient list is empty, **When** the user views the ingredient screen, **Then** a helpful prompt encourages them to start adding ingredients.
4. **Given** the user has added ingredients, **When** they navigate away and return within the same session, **Then** their ingredient list is still present.
5. **Given** an ingredient already exists in the list (case-insensitive match), **When** the user adds the same ingredient again, **Then** the original entry is kept and the duplicate attempt is ignored.

---

### User Story 2 — Request Meal Suggestions (Priority: P2)

A user who has populated their ingredient list navigates to the meal request screen, optionally adds dietary preferences or constraints (e.g., vegetarian, gluten-free, low-sodium), and submits the request to receive AI-generated meal ideas.

**Why this priority**: Submitting a request to the AI is the core value action of the app; it directly delivers the meal-suggestion outcome users came for.

**Independent Test**: Can be tested independently by pre-populating the session with ingredients, navigating to the meal request screen, entering a dietary preference, submitting, and verifying the results screen appears with content.

**Acceptance Scenarios**:

1. **Given** the user has at least one ingredient in their list, **When** they navigate to the meal request screen, **Then** their current ingredient list is shown for review.
2. **Given** the meal request screen is displayed, **When** the user enters dietary preferences and submits, **Then** the request is forwarded to the AI with both the ingredient list and the preferences included.
3. **Given** the user submits a request with no dietary preferences, **Then** the system uses the ingredient list alone and returns suggestions without error.
4. **Given** the user attempts to submit with an empty ingredient list, **Then** the system presents a clear validation message directing them to add ingredients first.

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

---

### Edge Cases

- What happens when the user adds duplicate ingredients? The system performs case-insensitive deduplication, keeps the first occurrence, and ignores later repeats.
- What happens when the AI API key is missing or invalid? The application fails gracefully at startup with a clear configuration error rather than crashing mid-request.
- What happens when the AI response takes longer than expected? The user sees a loading indicator; after a configurable timeout the system shows an error and offers a retry.
- What happens when the ingredient list exceeds a reasonable length? The system accepts up to 50 ingredients per session to keep prompts focused and avoid API token limits.
- What happens when dietary preferences text is excessively long? Input is capped at 500 characters with a visible character counter.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to add ingredients to a session-scoped ingredient list from a dedicated ingredient management screen.
- **FR-002**: Users MUST be able to remove any ingredient from their current session list.
- **FR-003**: The system MUST display the current ingredient list on both the ingredient screen and the meal request screen so users can confirm what will be submitted.
- **FR-004**: The system MUST accept optional free-text dietary preferences or constraints on the meal request screen (e.g., "vegetarian", "no nuts", "low carb").
- **FR-005**: The system MUST prevent submission of a meal request when the ingredient list is empty, displaying a clear validation message.
- **FR-006**: The system MUST forward the combined ingredient list and dietary preferences to the Anthropic Claude API and retrieve meal suggestions.
- **FR-007**: The results screen MUST display each AI-generated suggestion with at minimum: a recipe name, a brief description, and step-by-step cooking instructions.
- **FR-008**: The system MUST handle AI API errors gracefully, showing a user-friendly message and a retry option rather than an unhandled error page.
- **FR-009**: The Anthropic Claude API key MUST be supplied via an environment variable; the application MUST refuse to start without it.
- **FR-010**: The ingredient list MUST be limited to 50 items per session to maintain prompt quality and performance.
- **FR-011**: Dietary preference input MUST be capped at 500 characters.
- **FR-012**: The interface MUST be responsive and usable on mobile-sized screens (320 px and above) without horizontal scrolling.
- **FR-013**: The application MUST use a warm color palette and system font stack appropriate for a food and cooking context.
- **FR-014**: A successful AI response MUST contain exactly 3 suggestion items from the AI; responses with fewer or more than 3 suggestions MUST be handled as an error state with clear retry messaging.
- **FR-015**: If any of the 3 returned suggestion items are malformed, the system MUST drop only malformed items, display the remaining valid suggestions, and show a clear warning that some suggestions were omitted.
- **FR-016**: Ingredient entries MUST be unique by case-insensitive name; when a duplicate is submitted, the system MUST keep the first occurrence and ignore later repeats.

### Key Entities

- **Ingredient**: A named food item contributed by the user. Attributes: name (string). Identity rule: unique by case-insensitive name within a session list; first-entered value is retained. Scope: session-lifetime only; not persisted to a database.
- **MealRequest**: The payload submitted to the AI. Attributes: ingredient list (ordered collection of Ingredient names), dietary preferences (optional free text). Created at submission time.
- **MealSuggestion**: One AI-generated dish returned in the response. Attributes: recipe name, brief description, ordered list of cooking steps.
- **MealResponse**: The structured collection of MealSuggestion items returned from one AI call, associated with a MealRequest.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can add an ingredient and see it reflected in the list in under 1 second under normal network conditions.
- **SC-002**: Users can complete the full flow — add ingredients, set preferences, submit, view results — in under 3 minutes on first use.
- **SC-003**: Meal suggestion results are returned and displayed within 15 seconds of submission under typical API response times.
- **SC-004**: The application displays a meaningful error message (not a raw stack trace) 100% of the time when the AI service is unavailable.
- **SC-005**: The results screen is usable on a 375 px wide mobile screen with no horizontal scrolling required and all key content visible without zooming.
- **SC-006**: For successful responses with 3 well-formed suggestions, exactly 3 distinct meal suggestions are displayed, each with a name, description, and at least 3 cooking steps.
- **SC-007**: When a successful 3-item response contains malformed suggestion items, 100% of malformed items are excluded from display, valid items are still shown, and a visible warning indicates that some suggestions were omitted.
- **SC-008**: The application starts and runs without error when a valid API key is supplied, and produces a clear startup failure message when the key is absent.
- **SC-009**: When users attempt to add duplicate ingredients that differ only by case, 100% of duplicate attempts are ignored and the originally added ingredient remains unchanged in the list.

---

## Assumptions

- Users have a modern web browser (Chrome, Firefox, Safari, Edge — last 2 major versions); no IE11 support is required.
- No user account system or persistent database is needed; all state lives in the HTTP session within a single browser session.
- The Anthropic Claude API is accessed over HTTPS from the server side; the API key is never exposed to the browser.
- A single deployment environment is assumed (local development or single-server deploy); multi-instance session sharing is out of scope.
- The application will serve one user at a time per session; concurrent-user load testing is out of scope for v1.
- Recipes are presented as returned by the AI; the application does not validate nutritional content or safety of suggestions.
- The app will be packaged as a self-contained runnable JAR; no external web server setup is required.
- Ingredient deduplication is case-insensitive and first-write-wins (e.g., if "Chicken" is added first, later "chicken" is ignored).
- No image assets are required; the UI relies entirely on typography, spacing, and color for visual hierarchy.

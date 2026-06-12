# Research: Cusina AI — Phase 0 Findings

**Feature**: `001-meal-suggestion-webapp`  
**Date**: 2025-07-22  
**Status**: Complete — all NEEDS CLARIFICATION items resolved

---

## 1. Anthropic Java SDK Integration

### Decision
Use the official **`com.anthropic:anthropic-java:2.40.1`** SDK (MIT, Java 8+) for all Claude API calls. Do **not** use Spring WebClient / RestClient for the Anthropic API.

### Rationale
- Official SDK from Anthropic; actively maintained; no known Spring Boot 3 incompatibilities
- Handles auth headers (`x-api-key`, `anthropic-version: 2023-06-01`) automatically
- Provides both sync and async (`CompletableFuture`) APIs in a single artifact
- Jackson 2.18.x (already managed by Spring Boot BOM) is the transport serializer; no version conflict
- Kotlin stdlib transitive dependency is inert in a pure-Java Spring app

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| Spring AI (`spring-ai-anthropic-spring-boot-starter`) | Adds a full AI abstraction layer that's unnecessary for a single-API integration; opinionated `ChatClient` model may complicate structured JSON parsing |
| Raw `RestTemplate` / `RestClient` against `https://api.anthropic.com/v1/messages` | Requires manual header management, JSON request building, and response parsing — the SDK already handles all of this |

### Maven Coordinate
```xml
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>
    <version>2.40.1</version>
</dependency>
```

---

## 2. Structured JSON Output from Claude

### Decision
Use **Approach B — System Prompt JSON** (not the Beta SDK `outputConfig()`).

Instruct Claude via a system prompt to return only valid JSON matching the `MealResponse` schema. Parse the response with Jackson's `ObjectMapper`.

### Rationale
- The Beta Structured Outputs feature (`client.beta().messages()`) is still in beta; the `com.anthropic.models.beta.messages` package may change across SDK versions
- Approach B (system prompt + Jackson) is battle-tested, simpler to unit-test, and has no beta API dependency
- Spring Boot's auto-configured `ObjectMapper` (with `spring.jackson.deserialization.fail-on-unknown-properties=false`) handles edge cases gracefully

### Prompt Strategy
```
System prompt:
  "You are a creative meal planning chef. You MUST respond ONLY with valid JSON — 
   no markdown, no explanation, no code fences. Your response must match this schema:
   {
     \"meals\": [
       {
         \"name\": \"string\",
         \"description\": \"string (2-3 sentences)\",
         \"cookingTimeMinutes\": number,
         \"ingredients\": [\"string\"],
         \"steps\": [\"string\"]
       }
     ]
   }
   Always return exactly 3 meal suggestions."

User message:
  "Available ingredients: <comma-separated list>.
   Dietary preferences: <preferences or 'none'>.
   Suggest 3 practical, delicious meals a home cook can make with these ingredients."
```

**Validation**: If `ObjectMapper.readValue()` throws, the service catches the exception and returns a user-friendly error result rather than a 500 page.

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| Beta SDK `outputConfig(MealSuggestionList.class)` | Beta API risk; requires `client.beta().messages()` namespace which may change |
| Manual regex JSON extraction | Brittle; prompt engineering is more reliable with Claude |

---

## 3. Claude Model Selection

### Decision
**`claude-haiku-4-5`** as default model (configurable via `application.properties`).

### Rationale
- $1 / $5 per MTok (input / output) — 3–5× cheaper than Sonnet/Opus
- ~2–4 s typical response time vs. 8–15 s for Opus
- Meal suggestions are a straightforward generative task, not a complex reasoning task — Haiku quality is fully sufficient
- Model is configurable (`anthropic.model=claude-haiku-4-5`) so operators can upgrade to Sonnet 4.6 if richer descriptions are desired

### Alternatives Considered
| Model | Why Not Default |
|---|---|
| `claude-sonnet-4-6` | 3× more expensive; suitable if recipe descriptions need to be more elaborate |
| `claude-opus-4-8` | 5× more expensive; overkill for meal suggestions |

---

## 3b. Response Validation Policy (Clarified Spec)

### Decision
Apply a strict two-step validator after JSON parsing:
1. Raw AI payload must contain **exactly 3** suggestions.
2. Each suggestion must include non-blank `name`, `description`, and non-empty `steps`; malformed items are dropped and a warning is shown.

### Rationale
- Directly enforces FR-014/FR-015 and acceptance clarifications
- Separates transport success from business-valid success
- Preserves usable results when partial malformation occurs, without presenting invalid data as fully successful

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| Accept 1..N suggestions as success | Violates clarified exact-3 requirement |
| Fail entire response on any malformed item | Conflicts with clarified behavior to show valid suggestions and warning |

---

## 4. Threading Model for Long-Running AI Calls

### Decision
Use **`@Async` with a bounded `ThreadPoolTaskExecutor`** (Java 17 — no virtual threads). The `MealSuggestionService.suggest()` method is annotated `@Async`, returns `CompletableFuture<MealResponse>`, and is awaited in the controller with a `get(30, TimeUnit.SECONDS)` timeout guard.

### Rationale
- Spring MVC on Tomcat uses a servlet thread pool (default 200 threads); a 15 s blocking call would exhaust threads under moderate concurrent load
- `@Async` moves the blocking SDK call to a separate executor pool, freeing servlet threads immediately
- `CompletableFuture.get(timeout)` in the controller provides a hard timeout that surfaces as a user-friendly error page
- Java 21 virtual threads would make this even simpler (`Executors.newVirtualThreadPerTaskExecutor()`), but are out of scope for Java 17

### Executor Configuration
```java
@Bean("aiTaskExecutor")
public Executor aiTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("ai-task-");
    executor.initialize();
    return executor;
}
```

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| Synchronous call on servlet thread | Acceptable for single-user dev, but risks thread exhaustion under any concurrent load |
| Spring WebFlux (reactive stack) | Would require rewriting all controllers as reactive; over-engineering for Thymeleaf MVC app |
| `AnthropicOkHttpClientAsync` + `Mono.fromFuture()` | Requires WebFlux on classpath; not compatible with Thymeleaf / MVC without additional bridge |
| Java 21 virtual threads | Available, but `@Async` with traditional executor pool is sufficient for v1; virtual threads can be adopted in future optimization pass |

---

## 5. Spring Boot 3 Session Management

### Decision
Use a **`@SessionScope` Spring bean (`IngredientSession`)** to hold the ingredient list. This is cleaner than raw `HttpSession` attribute manipulation in controllers and allows the session state to be unit-tested without a mock session.

### Rationale
- `@SessionScope` creates one bean instance per HTTP session and is automatically garbage-collected when the session expires
- Encapsulates all ingredient mutation logic (add/dedup/remove/cap) in one place
- `HttpSessionEventPublisher` is registered in `CusinaAiApplication` to ensure session lifecycle events fire correctly with Spring Security (not used here) and Spring's session tracking

### Deduplication Strategy
Ingredients are stored in a `LinkedHashSet<String>` (insertion-ordered, unique). Keys are normalized to lower-case trimmed strings for comparison, but the display value preserves the user's original casing of the first insertion.

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| `@SessionAttributes` on each controller | Fragile across multiple controllers; `IngredientSession` shared across `IngredientController` and `MealController` |
| Raw `HttpSession.setAttribute()` | No type safety; harder to unit-test |

---

## 6. Fail-Fast API Key Validation

### Decision
Implement `AppStartupValidator` as a `@Component` implementing `ApplicationListener<ApplicationReadyEvent>`. On startup, it reads `anthropic.api-key`; if blank/null, it logs a fatal message and calls `SpringApplication.exit()` with exit code 1.

### Rationale
- Spec FR-009 and SC-007 require the app to "refuse to start" without a valid key
- `@ConfigurationProperties` with `@NotBlank` + `spring.profiles.active=prod` fail-fast binding is the Spring Boot idiomatic way, but `ApplicationReadyEvent` gives a better user-facing message with instructions
- Does not make a live API call at startup (avoiding unnecessary cost/latency)

---

## 7. Thymeleaf Form Patterns

### Decision
Use **standard Spring MVC form post + PRG (Post-Redirect-Get)** pattern for ingredient management. No AJAX / Fetch API calls. Meal suggestion submission uses a standard form POST that blocks until the `CompletableFuture` resolves (up to 30 s) and then redirects to the results view.

### Rationale
- Keeps JavaScript to zero (spec does not require AJAX; Thymeleaf is server-rendered)
- PRG pattern prevents duplicate form submissions on browser back/refresh
- Thymeleaf fragments (`th:replace`) used for the shared page layout to avoid HTML duplication

---

## Resolved Unknowns Summary

| # | Unknown | Resolution |
|---|---------|------------|
| 1 | Anthropic Java SDK availability | Official `com.anthropic:anthropic-java:2.40.1` — use it |
| 2 | Structured JSON output from Claude | System-prompt JSON strategy + Jackson parse |
| 3 | Claude model for meal suggestions | `claude-haiku-4-5` (fast, cheap, sufficient quality) |
| 4 | Threading for 10–15 s API calls | `@Async` with `ThreadPoolTaskExecutor`; 30 s timeout |
| 5 | Session management for ingredient list | `@SessionScope` bean (`IngredientSession`) |
| 6 | Fail-fast on missing API key | `ApplicationReadyEvent` listener; exit code 1 |
| 7 | Form handling pattern | Standard PRG; no JavaScript |
| 8 | Exact-count and malformed-item policy | Require exactly 3 raw suggestions; drop malformed with warning |

package com.cusina.ai.session;

import java.util.Optional;

public interface IngredientSessionRepository {

    Optional<PersistedIngredientSession> load(String sessionKey);

    void save(String sessionKey, PersistedIngredientSession state);
}


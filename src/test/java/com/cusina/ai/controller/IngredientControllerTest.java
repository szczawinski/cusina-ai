package com.cusina.ai.controller;

import com.cusina.ai.session.IngredientSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(IngredientController.class)
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngredientSession ingredientSession;

    @Test
    void shouldRenderIngredientPageAndEmptyStateData() throws Exception {
        when(ingredientSession.getIngredients()).thenReturn(List.of());
        when(ingredientSession.isFull()).thenReturn(false);

        mockMvc.perform(get("/ingredients"))
                .andExpect(status().isOk())
                .andExpect(view().name("ingredients"))
                .andExpect(model().attributeExists("ingredients", "addForm", "isFull", "units"));

        verify(ingredientSession).initializeIfNeeded();
    }

    @Test
    void shouldAddIngredientAndRedirect() throws Exception {
        when(ingredientSession.addIngredient(anyString(), any(), any())).thenReturn(IngredientSession.AddResult.ADDED);

        mockMvc.perform(post("/ingredients/add")
                        .param("name", "Garlic")
                        .param("quantity", "200")
                        .param("unit", "g"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ingredients"))
                .andExpect(flash().attribute("successMessage", "Dodano składnik: „Garlic”."));
    }

    @Test
    void shouldRejectDuplicateIngredientWithPolishFlashMessage() throws Exception {
        when(ingredientSession.addIngredient(anyString(), any(), any())).thenReturn(IngredientSession.AddResult.DUPLICATE);

        mockMvc.perform(post("/ingredients/add").param("name", "Garlic"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ingredients"))
                .andExpect(flash().attribute("errorMessage", "Składnik „Garlic” już jest na liście."));
    }

    @Test
    void shouldRejectFractionalQuantityForPiecesUnit() throws Exception {
        mockMvc.perform(post("/ingredients/add")
                        .param("name", "Jajka")
                        .param("quantity", "1.5")
                        .param("unit", "szt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ingredients"))
                .andExpect(flash().attribute("errorMessage", "Dla jednostki \"szt\" podaj dodatnią liczbę całkowitą."));

        verify(ingredientSession, never()).addIngredient(anyString(), any(), any());
    }

    @Test
    void shouldRemoveIngredientAndRedirect() throws Exception {
        when(ingredientSession.removeIngredientAndReport("garlic")).thenReturn(true);

        mockMvc.perform(post("/ingredients/remove").param("normalizedKey", "garlic"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ingredients"))
                .andExpect(flash().attribute("successMessage", "Usunięto składnik z listy."));

        verify(ingredientSession).removeIngredientAndReport("garlic");
    }

    @Test
    void shouldSilentlyRedirectWhenRemovingMissingIngredient() throws Exception {
        when(ingredientSession.removeIngredientAndReport("garlic")).thenReturn(false);

        mockMvc.perform(post("/ingredients/remove").param("normalizedKey", "garlic"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ingredients"));

        verify(ingredientSession).removeIngredientAndReport("garlic");
        verify(ingredientSession, never()).removeIngredient(anyString());
    }
}


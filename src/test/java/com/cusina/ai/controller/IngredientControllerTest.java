package com.cusina.ai.controller;

import com.cusina.ai.session.IngredientSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .andExpect(model().attributeExists("ingredients", "addForm", "isFull"));
    }

    @Test
    void shouldAddIngredientAndRedirect() throws Exception {
        when(ingredientSession.addIngredient(anyString())).thenReturn(IngredientSession.AddResult.ADDED);

        mockMvc.perform(post("/ingredients/add").param("name", "Garlic"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ingredients"));
    }

    @Test
    void shouldRemoveIngredientAndRedirect() throws Exception {
        mockMvc.perform(post("/ingredients/remove").param("normalizedKey", "garlic"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ingredients"));

        verify(ingredientSession).removeIngredient("garlic");
    }
}


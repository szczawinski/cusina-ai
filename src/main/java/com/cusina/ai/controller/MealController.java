package com.cusina.ai.controller;

import com.cusina.ai.config.AnthropicProperties;
import com.cusina.ai.model.MealRequest;
import com.cusina.ai.model.MealResponse;
import com.cusina.ai.service.MealSuggestionService;
import com.cusina.ai.session.IngredientSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Controller
public class MealController {

    private final IngredientSession ingredientSession;
    private final MealSuggestionService mealSuggestionService;
    private final AnthropicProperties anthropicProperties;

    public MealController(IngredientSession ingredientSession,
                          MealSuggestionService mealSuggestionService,
                          AnthropicProperties anthropicProperties) {
        this.ingredientSession = ingredientSession;
        this.mealSuggestionService = mealSuggestionService;
        this.anthropicProperties = anthropicProperties;
    }

    @GetMapping("/meal-request")
    public String showMealRequest(Model model, RedirectAttributes redirectAttributes) {
        if (ingredientSession.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please add at least one ingredient before requesting meal suggestions.");
            return "redirect:/ingredients";
        }

        if (!model.containsAttribute("mealRequest")) {
            model.addAttribute("mealRequest", new MealRequest());
        }
        model.addAttribute("ingredients", ingredientSession.getIngredients());
        model.addAttribute("charLimit", anthropicProperties.getDietaryPreferenceMaxLength());
        return "meal-request";
    }

    @PostMapping("/meal-request/suggest")
    public String suggestMeals(@Valid @ModelAttribute("mealRequest") MealRequest mealRequest,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (ingredientSession.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please add at least one ingredient before requesting meal suggestions.");
            return "redirect:/ingredients";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("ingredients", ingredientSession.getIngredients());
            model.addAttribute("charLimit", anthropicProperties.getDietaryPreferenceMaxLength());
            return "meal-request";
        }

        MealResponse response;
        try {
            response = mealSuggestionService
                    .suggest(ingredientSession.getIngredientNames(), mealRequest.getDietaryPreferences())
                    .get(anthropicProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            response = MealResponse.error("The AI took too long to respond. Please try again.");
        } catch (ExecutionException ex) {
            response = MealResponse.error("Unable to reach the AI service. Please try again.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            response = MealResponse.error("The request was interrupted. Please try again.");
        }

        redirectAttributes.addFlashAttribute("mealResponse", response);
        return "redirect:/results";
    }

    @GetMapping("/results")
    public String showResults(Model model) {
        if (!model.containsAttribute("mealResponse")) {
            return "redirect:/meal-request";
        }
        return "results";
    }
}

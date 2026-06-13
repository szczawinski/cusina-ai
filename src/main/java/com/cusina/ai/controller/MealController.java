package com.cusina.ai.controller;

import com.cusina.ai.config.AnthropicProperties;
import com.cusina.ai.controller.form.MealRequestForm;
import com.cusina.ai.model.DietType;
import com.cusina.ai.model.DishType;
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
            redirectAttributes.addFlashAttribute("errorMessage", "Najpierw dodaj co najmniej jeden składnik.");
            return "redirect:/ingredients";
        }

        if (!model.containsAttribute("mealRequest")) {
            model.addAttribute("mealRequest", new MealRequestForm());
        }
        model.addAttribute("ingredients", ingredientSession.getIngredients());
        model.addAttribute("charLimit", anthropicProperties.getDietaryPreferenceMaxLength());
        model.addAttribute("dishTypes", DishType.values());
        model.addAttribute("dietTypes", DietType.values());
        return "meal-request";
    }

    @PostMapping("/meal-request/suggest")
    public String suggestMeals(@Valid @ModelAttribute("mealRequest") MealRequestForm mealRequestForm,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (ingredientSession.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Najpierw dodaj co najmniej jeden składnik.");
            return "redirect:/ingredients";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("ingredients", ingredientSession.getIngredients());
            model.addAttribute("charLimit", anthropicProperties.getDietaryPreferenceMaxLength());
            model.addAttribute("dishTypes", DishType.values());
            model.addAttribute("dietTypes", DietType.values());
            return "meal-request";
        }

        MealRequest mealRequest = new MealRequest();
        mealRequest.setIngredients(ingredientSession.getIngredientNames());
        mealRequest.setIngredientDetails(ingredientSession.getIngredientPromptDetails());
        mealRequest.setDietaryPreferences(mealRequestForm.getDietaryPreferences());
        DishType.fromValue(mealRequestForm.getDishType()).ifPresent(mealRequest::setDishType);
        DietType.fromValue(mealRequestForm.getDietType()).ifPresent(mealRequest::setDietType);
        mealRequest.setLocale("pl-PL");

        MealResponse response;
        try {
            response = mealSuggestionService
                    .suggest(mealRequest)
                    .get(anthropicProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            response = MealResponse.error("Odpowiedź AI trwała zbyt długo. Spróbuj ponownie.");
        } catch (ExecutionException ex) {
            response = MealResponse.error("Nie udało się połączyć z usługą AI. Spróbuj ponownie.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            response = MealResponse.error("Żądanie zostało przerwane. Spróbuj ponownie.");
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

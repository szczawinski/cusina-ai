package com.cusina.ai.controller;

import com.cusina.ai.config.AnthropicProperties;
import com.cusina.ai.controller.form.MealRequestForm;
import com.cusina.ai.model.DietType;
import com.cusina.ai.model.DishType;
import com.cusina.ai.model.MealRequest;
import com.cusina.ai.model.MealResponse;
import com.cusina.ai.service.MealSuggestionService;
import com.cusina.ai.session.IngredientSession;
import com.cusina.ai.session.MealSelectionSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Controller
public class MealController {

    private final IngredientSession ingredientSession;
    private final MealSuggestionService mealSuggestionService;
    private final AnthropicProperties anthropicProperties;
    private final MealSelectionSession mealSelectionSession;

    public MealController(IngredientSession ingredientSession,
                          MealSuggestionService mealSuggestionService,
                          AnthropicProperties anthropicProperties,
                          MealSelectionSession mealSelectionSession) {
        this.ingredientSession = ingredientSession;
        this.mealSuggestionService = mealSuggestionService;
        this.anthropicProperties = anthropicProperties;
        this.mealSelectionSession = mealSelectionSession;
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

        if (response.hasError()) {
            mealSelectionSession.updateLatestSuggestions(List.of());
        } else {
            mealSelectionSession.updateLatestSuggestions(response.getMeals());
        }

        redirectAttributes.addFlashAttribute("mealResponse", response);
        return "redirect:/results";
    }

    @GetMapping("/results")
    public String showResults(Model model) {
        if (model.containsAttribute("mealResponse")) {
            MealResponse response = (MealResponse) model.getAttribute("mealResponse");
            if (response != null && !response.hasError()) {
                mealSelectionSession.updateLatestSuggestions(response.getMeals());
            }
            return "results";
        }

        if (!mealSelectionSession.hasLatestSuggestions()) {
            return "redirect:/meal-request";
        }

        MealResponse fallback = new MealResponse();
        fallback.setRawCount(mealSelectionSession.getLatestSuggestions().size());
        fallback.setMeals(mealSelectionSession.getLatestSuggestions());
        model.addAttribute("mealResponse", fallback);
        return "results";
    }

    @PostMapping("/results/select")
    public String selectMeal(@RequestParam("index") int index, RedirectAttributes redirectAttributes) {
        if (mealSelectionSession.selectFromLatest(index).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nie udało się wybrać przepisu. Spróbuj ponownie.");
            return "redirect:/results";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Przepis został dodany do historii.");
        return "redirect:/meal-selected";
    }

    @GetMapping("/meal-selected")
    public String showSelectedMeal(Model model, RedirectAttributes redirectAttributes) {
        return mealSelectionSession.getSelectedMeal()
                .map(selected -> {
                    model.addAttribute("selectedMeal", selected);
                    return "meal-selected";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Najpierw wybierz przepis z listy sugestii.");
                    return "redirect:/results";
                });
    }

    @GetMapping("/meal-history")
    public String showMealHistory(Model model) {
        model.addAttribute("mealHistory", mealSelectionSession.getSelectedMealsHistory());
        model.addAttribute("selectionCount", mealSelectionSession.getSelectionCount());
        model.addAttribute("uniqueMealCount", mealSelectionSession.getUniqueMealCount());
        return "meal-history";
    }
}

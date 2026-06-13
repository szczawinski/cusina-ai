package com.cusina.ai.controller;

import com.cusina.ai.controller.form.IngredientForm;
import com.cusina.ai.model.IngredientUnit;
import com.cusina.ai.session.IngredientSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class IngredientController {

    private final IngredientSession ingredientSession;

    public IngredientController(IngredientSession ingredientSession) {
        this.ingredientSession = ingredientSession;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/ingredients";
    }

    @GetMapping("/ingredients")
    public String showIngredients(Model model) {
        ingredientSession.initializeIfNeeded();
        if (!model.containsAttribute("addForm")) {
            model.addAttribute("addForm", new IngredientForm());
        }
        model.addAttribute("ingredients", ingredientSession.getIngredients());
        model.addAttribute("isFull", ingredientSession.isFull());
        model.addAttribute("units", IngredientUnit.values());
        model.addAttribute("preloadedCount", 10);
        return "ingredients";
    }

    @PostMapping("/ingredients/add")
    public String addIngredient(@Valid @ModelAttribute("addForm") IngredientForm form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            redirectAttributes.addFlashAttribute("addForm", form);
            return "redirect:/ingredients";
        }

        IngredientSession.AddResult addResult = ingredientSession.addIngredient(form.getName(), form.getQuantity(), form.getUnit());
        String ingredientName = form.getName() == null ? "" : form.getName().trim();
        switch (addResult) {
            case ADDED -> redirectAttributes.addFlashAttribute("successMessage", "Dodano składnik: „" + ingredientName + "”.");
            case DUPLICATE -> redirectAttributes.addFlashAttribute("errorMessage", "Składnik „" + ingredientName + "” już jest na liście.");
            case FULL -> redirectAttributes.addFlashAttribute("errorMessage", "Osiągnięto limit 50 składników w sesji.");
            default -> redirectAttributes.addFlashAttribute("errorMessage", "Nazwa składnika nie może być pusta.");
        }
        return "redirect:/ingredients";
    }

    @PostMapping("/ingredients/remove")
    public String removeIngredient(@RequestParam("normalizedKey") String normalizedKey,
                                   RedirectAttributes redirectAttributes) {
        boolean removed = ingredientSession.removeIngredientAndReport(normalizedKey);
        if (removed) {
            redirectAttributes.addFlashAttribute("successMessage", "Usunięto składnik z listy.");
        }
        return "redirect:/ingredients";
    }
}


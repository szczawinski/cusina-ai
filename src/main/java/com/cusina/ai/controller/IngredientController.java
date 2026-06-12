package com.cusina.ai.controller;

import com.cusina.ai.controller.form.IngredientForm;
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
        if (!model.containsAttribute("addForm")) {
            model.addAttribute("addForm", new IngredientForm());
        }
        model.addAttribute("ingredients", ingredientSession.getIngredients());
        model.addAttribute("isFull", ingredientSession.isFull());
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

        IngredientSession.AddResult addResult = ingredientSession.addIngredient(form.getName());
        switch (addResult) {
            case ADDED -> redirectAttributes.addFlashAttribute("successMessage", "'" + form.getName().trim() + "' added.");
            case DUPLICATE -> redirectAttributes.addFlashAttribute("errorMessage", "'" + form.getName().trim() + "' is already in your list.");
            case FULL -> redirectAttributes.addFlashAttribute("errorMessage", "You have reached the maximum of 50 ingredients.");
            default -> redirectAttributes.addFlashAttribute("errorMessage", "Ingredient name cannot be empty.");
        }
        return "redirect:/ingredients";
    }

    @PostMapping("/ingredients/remove")
    public String removeIngredient(@RequestParam("normalizedKey") String normalizedKey) {
        ingredientSession.removeIngredient(normalizedKey);
        return "redirect:/ingredients";
    }
}


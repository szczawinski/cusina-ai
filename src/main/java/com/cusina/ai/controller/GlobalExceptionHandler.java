package com.cusina.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handleException(Exception exception, Model model) {
        log.error("Nieoczekiwany błąd aplikacji", exception);
        model.addAttribute("status", 500);
        model.addAttribute("message", "Wystąpił nieoczekiwany błąd. Spróbuj ponownie.");
        return "error";
    }
}


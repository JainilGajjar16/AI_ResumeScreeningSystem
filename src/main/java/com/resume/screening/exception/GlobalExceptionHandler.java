package com.resume.screening.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex, Model model) {
        System.err.println("=== GLOBAL EXCEPTION HANDLER (IllegalArgumentException) ===");
        ex.printStackTrace();
        log.warn("IllegalArgumentException caught: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        System.err.println("=== GLOBAL EXCEPTION HANDLER (Exception) ===");
        ex.printStackTrace();
        log.error("Unhandled exception caught in GlobalExceptionHandler: ", ex);
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred. Please try again later.");
        return "error";
    }
}

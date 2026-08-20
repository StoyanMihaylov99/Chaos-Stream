package com.portfolio.chaosstream.validation.model;

import java.util.List;

public record ValidationResult(boolean isValid, List<String> violations) {

    public static ValidationResult passed() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failed(List<String> violations) {
        return new ValidationResult(false, violations);
    }
}

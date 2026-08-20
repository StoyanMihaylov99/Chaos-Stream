package com.portfolio.chaosstream.validation.service;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.validation.model.ValidationResult;
import com.portfolio.chaosstream.validation.service.rule.TransactionValidationRule;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionValidationService {

    private final List<TransactionValidationRule> rules;

    public TransactionValidationService(List<TransactionValidationRule> rules){
        this.rules = rules;
    }

    public ValidationResult validate(TransactionEvent event) {
        List<String> violations = new ArrayList<>();
        for (TransactionValidationRule rule : rules) {
            rule.validate(event).ifPresent(violations::add);
        }
        return violations.isEmpty() ? ValidationResult.passed() : ValidationResult.failed(violations);
    }

}

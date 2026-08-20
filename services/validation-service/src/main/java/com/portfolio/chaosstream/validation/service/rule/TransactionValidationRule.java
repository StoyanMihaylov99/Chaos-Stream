package com.portfolio.chaosstream.validation.service.rule;

import com.portfolio.chaosstream.model.TransactionEvent;

import java.util.Optional;

public interface TransactionValidationRule {

    Optional<String> validate(TransactionEvent event);
}

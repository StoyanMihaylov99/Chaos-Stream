package com.portfolio.chaosstream.validation.service.rule;

import com.portfolio.chaosstream.model.TransactionEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DuplicateTransactionRule implements TransactionValidationRule {

    private final Set<String> seenIdempotencyKeys = ConcurrentHashMap.newKeySet();

    @Override
    public Optional<String> validate(TransactionEvent event) {
        String idempotencyKey = event.payload().idempotencyKey();
        if (!seenIdempotencyKeys.add(idempotencyKey)) {
            return Optional.of("Duplicate transaction with idempotency key: " + idempotencyKey);
        }
        return Optional.empty();
    }
}

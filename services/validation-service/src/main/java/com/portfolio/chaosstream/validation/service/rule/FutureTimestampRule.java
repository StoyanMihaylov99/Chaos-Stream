package com.portfolio.chaosstream.validation.service.rule;

import com.portfolio.chaosstream.model.TransactionEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class FutureTimestampRule implements TransactionValidationRule {

    private static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofMinutes(1);

    @Override
    public Optional<String> validate(TransactionEvent event) {
        LocalDateTime timestamp = event.metaData().timestamp();
        if (timestamp.isAfter(LocalDateTime.now().plus(CLOCK_SKEW_TOLERANCE))) {
            return Optional.of("Transaction timestamp is in the future: " + timestamp);
        }
        return Optional.empty();
    }
}

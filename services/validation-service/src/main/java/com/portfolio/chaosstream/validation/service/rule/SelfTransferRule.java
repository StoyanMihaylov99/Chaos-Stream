package com.portfolio.chaosstream.validation.service.rule;

import com.portfolio.chaosstream.model.TransactionEvent;
import com.portfolio.chaosstream.model.TransactionEvent.Payload;
import com.portfolio.chaosstream.model.TransactionEvent.TransactionType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SelfTransferRule implements TransactionValidationRule {

    @Override
    public Optional<String> validate(TransactionEvent event) {
        Payload payload = event.payload();
        if (payload.transactionType() == TransactionType.TRANSFER
                && payload.senderAccount().equals(payload.receiverAccount())) {
            return Optional.of("Sender and receiver account must not be the same for a transfer");
        }
        return Optional.empty();
    }
}

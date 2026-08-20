package com.portfolio.chaosstream.validation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.portfolio.chaosstream.model.TransactionEvent;

import java.util.List;

public record RejectedTransactionEvent(@JsonProperty("transaction") TransactionEvent transaction,
                                        @JsonProperty("violations") List<String> violations) {
}

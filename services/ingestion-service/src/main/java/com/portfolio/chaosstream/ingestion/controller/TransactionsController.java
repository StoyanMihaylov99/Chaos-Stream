package com.portfolio.chaosstream.ingestion.controller;

import com.portfolio.chaosstream.ingestion.service.TransactionProducerService;
import com.portfolio.chaosstream.model.TransactionEvent;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/transactions")
public class TransactionsController {

    public final TransactionProducerService transactionProducerService;

    public TransactionsController(TransactionProducerService transactionProducerService) {
        this.transactionProducerService = transactionProducerService;
    }

    @PostMapping
    public ResponseEntity<String> incomingTransactions(@Valid @RequestBody TransactionEvent transactionEvent) {
        transactionProducerService.publish(transactionEvent);
        return ResponseEntity.accepted().body(transactionEvent.metaData().eventId());
    }


}

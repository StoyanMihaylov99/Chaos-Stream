package com.portfolio.chaosstream.storage.controller;

import com.portfolio.chaosstream.storage.dto.TransactionResponse;
import com.portfolio.chaosstream.storage.service.TransactionQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/transactions")
public class TransactionsController {

    private final TransactionQueryService transactionQueryService;

    public TransactionsController(TransactionQueryService transactionQueryService) {
        this.transactionQueryService = transactionQueryService;
    }

    @GetMapping("/{idempotencyKey}")
    public TransactionResponse getByIdempotencyKey(@PathVariable String idempotencyKey) {
        return transactionQueryService.findByIdempotencyKey(idempotencyKey);
    }

    @GetMapping
    public Page<TransactionResponse> getAll(Pageable pageable) {
        return transactionQueryService.findAll(pageable);
    }

}

package com.portfolio.chaosstream.gateway.support;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stub target for the test-only "transaction-route" forward, standing in for the
 * real downstream transaction service so gateway filter-chain tests don't need a
 * live backend.
 */
@RestController
public class TestTransactionsController {

    @GetMapping("/internal/test/transactions")
    public ResponseEntity<Void> ok() {
        return ResponseEntity.ok().build();
    }
}

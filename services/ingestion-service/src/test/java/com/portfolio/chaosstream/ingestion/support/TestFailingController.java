package com.portfolio.chaosstream.ingestion.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestFailingController {

    @GetMapping("/internal/test/unexpected-exception")
    public void throwUnexpectedException() {
        throw new IllegalStateException("boom");
    }
}

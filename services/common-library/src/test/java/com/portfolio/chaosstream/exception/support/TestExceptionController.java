package com.portfolio.chaosstream.exception.support;

import com.portfolio.chaosstream.exception.ApplicationException;
import com.portfolio.chaosstream.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestExceptionController {

    @GetMapping("/internal/test/application-exception")
    public void throwApplicationException() {
        throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @GetMapping("/internal/test/application-exception-custom-message")
    public void throwApplicationExceptionWithCustomMessage() {
        throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "Downstream ledger is unreachable");
    }

    @GetMapping("/internal/test/unexpected-exception")
    public void throwUnexpectedException() {
        throw new IllegalStateException("boom");
    }
}

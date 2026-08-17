package com.portfolio.chaosstream.exception.support;

import com.portfolio.chaosstream.exception.ApplicationException;
import com.portfolio.chaosstream.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
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

    @PostMapping("/internal/test/validated-body")
    public void validateBody(@Valid @RequestBody TestRequest request) {
        // no-op: reaching here means validation passed
    }

    @GetMapping("/internal/test/validated-param/{amount}")
    public void validateParam(@PathVariable @Min(1) int amount) {
        // no-op: reaching here means validation passed
    }

    public record TestRequest(@NotBlank(message = "reference must not be blank") String reference,
                               @Min(value = 1, message = "amount must be at least 1") int amount) {
    }
}

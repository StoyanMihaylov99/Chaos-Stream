package com.portfolio.chaosstream.exception;

import com.portfolio.chaosstream.exception.support.TestExceptionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestExceptionController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void applicationException_mapsToItsErrorCodeAndStatus() throws Exception {
        mockMvc.perform(get("/internal/test/application-exception"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(ErrorCode.SERVICE_UNAVAILABLE.getDefaultMessage()))
                .andExpect(jsonPath("$.path").value("/internal/test/application-exception"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(header().exists(TraceIdSupport.HEADER));
    }

    @Test
    void incomingTraceIdHeader_isEchoedBackInBodyAndResponseHeader() throws Exception {
        mockMvc.perform(get("/internal/test/application-exception")
                        .header(TraceIdSupport.HEADER, "test-trace-id-123"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.traceId").value("test-trace-id-123"))
                .andExpect(header().string(TraceIdSupport.HEADER, "test-trace-id-123"));
    }

    @Test
    void applicationException_usesCustomMessageWhenProvided() throws Exception {
        mockMvc.perform(get("/internal/test/application-exception-custom-message"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Downstream ledger is unreachable"));
    }

    @Test
    void unhandledException_fallsBackToInternalServerError() throws Exception {
        mockMvc.perform(get("/internal/test/unexpected-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()));
    }

    @Test
    void invalidRequestBody_mapsToValidationFailedWithFieldErrors() throws Exception {
        mockMvc.perform(post("/internal/test/validated-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\": \"\", \"amount\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(ErrorCode.VALIDATION_FAILED.getDefaultMessage()))
                .andExpect(jsonPath("$.validationErrors.reference[0]").value("reference must not be blank"))
                .andExpect(jsonPath("$.validationErrors.amount[0]").value("amount must be at least 1"));
    }

    @Test
    void invalidPathVariable_mapsToValidationFailedWithFieldErrors() throws Exception {
        mockMvc.perform(get("/internal/test/validated-param/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.amount").isArray())
                .andExpect(jsonPath("$.validationErrors.amount", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("must be greater than or equal to 1"))));
    }
}

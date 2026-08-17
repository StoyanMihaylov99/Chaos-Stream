package com.portfolio.chaosstream.exception;

import com.portfolio.chaosstream.exception.support.TestExceptionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(jsonPath("$.timestamp").exists());
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
}

package com.portfolio.chaosstream.ingestion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.chaosstream.exception.ApplicationException;
import com.portfolio.chaosstream.exception.ErrorCode;
import com.portfolio.chaosstream.exception.GlobalExceptionHandler;
import com.portfolio.chaosstream.ingestion.service.TransactionProducerService;
import com.portfolio.chaosstream.model.TransactionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.portfolio.chaosstream.model.TransactionEvent.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionsController.class)
@Import(GlobalExceptionHandler.class)
class TransactionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionProducerService transactionProducerService;

    @Test
    void validTransaction_publishesAndReturnsAccepted() throws Exception {
        // Given
        TransactionEvent event = setupTransactionEvent("ACC-1001");

        // When
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))

                // Then
                .andExpect(status().isAccepted())
                .andExpect(content().string(event.metaData().eventId()));
    }

    @Test
    void invalidTransaction_returnsValidationFailed() throws Exception {
        // Given
        String invalidPayload = """
                {
                  "metadata": {
                    "event_id": "",
                    "timestamp": "2026-08-19T10:15:30"
                  },
                  "payload": {
                    "transaction_type": "TRANSFER",
                    "amount": -10,
                    "currency": "USD",
                    "sender_account": "",
                    "receiver_account": "ACC-2002",
                    "status": "PENDING",
                    "idempotency_key": "idem-1"
                  },
                  "security": {
                    "user_id": "user_99",
                    "client_id": "transaction-producer-01"
                  }
                }
                """;

        // When
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.['metaData.eventId']").exists())
                .andExpect(jsonPath("$.validationErrors.['payload.senderAccount']").exists())
                .andExpect(jsonPath("$.validationErrors.['payload.amount']").exists());
    }

    @Test
    void producerFails_returnsServiceUnavailable() throws Exception {
        // Given
        TransactionEvent event = setupTransactionEvent("ACC-1001");
        doThrow(new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "Kafka broker did not acknowledge in time"))
                .when(transactionProducerService).publish(any());

        // When
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))

                // Then
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"));
    }

    private static TransactionEvent setupTransactionEvent(String senderAccount) {
        return builder()
                .metaData(MetaData.builder()
                        .eventId("evt-1")
                        .timestamp(LocalDateTime.now())
                        .build())
                .payload(Payload.builder()
                        .transactionType(TransactionType.TRANSFER)
                        .amount(BigDecimal.TEN)
                        .currency(Currency.USD)
                        .senderAccount(senderAccount)
                        .receiverAccount("ACC-2002")
                        .status(Status.PENDING)
                        .idempotencyKey("idem-1")
                        .build())
                .security(Security.builder()
                        .userId("user_99")
                        .clientId("transaction-producer-01")
                        .build())
                .build();
    }
}

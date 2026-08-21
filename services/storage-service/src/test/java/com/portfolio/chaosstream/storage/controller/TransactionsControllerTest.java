package com.portfolio.chaosstream.storage.controller;

import com.portfolio.chaosstream.exception.ApplicationException;
import com.portfolio.chaosstream.exception.ErrorCode;
import com.portfolio.chaosstream.exception.GlobalExceptionHandler;
import com.portfolio.chaosstream.model.TransactionEvent.Currency;
import com.portfolio.chaosstream.model.TransactionEvent.Status;
import com.portfolio.chaosstream.model.TransactionEvent.TransactionType;
import com.portfolio.chaosstream.storage.dto.TransactionResponse;
import com.portfolio.chaosstream.storage.service.TransactionQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionsController.class)
@Import(GlobalExceptionHandler.class)
class TransactionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionQueryService transactionQueryService;

    @Test
    void getByIdempotencyKey_found_returnsOkWithBody() throws Exception {
        TransactionResponse response = setupTransactionResponse("idem-1");
        when(transactionQueryService.findByIdempotencyKey("idem-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/transactions/idem-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotency_key").value("idem-1"))
                .andExpect(jsonPath("$.sender_account").value("1234"));
    }

    @Test
    void getByIdempotencyKey_notFound_returnsNotFoundWithJsonErrorBody() throws Exception {
        when(transactionQueryService.findByIdempotencyKey("missing"))
                .thenThrow(new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND,
                        "No transaction found with idempotency key: missing"));

        mockMvc.perform(get("/api/v1/transactions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getAll_returnsPagedResults() throws Exception {
        TransactionResponse response = setupTransactionResponse("idem-1");
        Page<TransactionResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);
        when(transactionQueryService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idempotency_key").value("idem-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private static TransactionResponse setupTransactionResponse(String idempotencyKey) {
        return TransactionResponse.builder()
                .eventId("evt-1")
                .timestamp(LocalDateTime.now())
                .transactionType(TransactionType.TRANSFER)
                .amount(BigDecimal.TEN)
                .currency(Currency.USD)
                .senderAccount("1234")
                .receiverAccount("5678")
                .status(Status.PENDING)
                .idempotencyKey(idempotencyKey)
                .userId("user_99")
                .clientId("transaction-producer-01")
                .build();
    }
}

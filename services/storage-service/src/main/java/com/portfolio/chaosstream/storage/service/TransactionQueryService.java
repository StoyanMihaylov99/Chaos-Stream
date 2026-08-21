package com.portfolio.chaosstream.storage.service;

import com.portfolio.chaosstream.exception.ApplicationException;
import com.portfolio.chaosstream.exception.ErrorCode;
import com.portfolio.chaosstream.storage.dto.TransactionResponse;
import com.portfolio.chaosstream.storage.entity.TransactionEntity;
import com.portfolio.chaosstream.storage.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;

    public TransactionQueryService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse findByIdempotencyKey(String idempotencyKey) {
        TransactionEntity entity = transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND,
                        "No transaction found with idempotency key: " + idempotencyKey));
        return TransactionResponse.from(entity);
    }

    public Page<TransactionResponse> findAll(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(TransactionResponse::from);
    }

}

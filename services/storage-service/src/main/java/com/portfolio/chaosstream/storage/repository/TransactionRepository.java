package com.portfolio.chaosstream.storage.repository;

import com.portfolio.chaosstream.storage.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);

}

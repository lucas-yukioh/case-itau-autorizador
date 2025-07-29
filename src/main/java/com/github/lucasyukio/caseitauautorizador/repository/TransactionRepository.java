package com.github.lucasyukio.caseitauautorizador.repository;

import com.github.lucasyukio.caseitauautorizador.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
}

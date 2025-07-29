package com.github.lucasyukio.caseitauautorizador.repository;

import com.github.lucasyukio.caseitauautorizador.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>, AccountBatchRepository {
}

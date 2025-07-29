package com.github.lucasyukio.caseitauautorizador.repository;

import com.github.lucasyukio.caseitauautorizador.model.Account;

import java.util.List;

public interface AccountBatchRepository {

    void saveAccountsBatch(List<Account> accounts);
}

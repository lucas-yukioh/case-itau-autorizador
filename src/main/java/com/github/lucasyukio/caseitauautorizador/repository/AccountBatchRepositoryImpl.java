package com.github.lucasyukio.caseitauautorizador.repository;

import com.github.lucasyukio.caseitauautorizador.model.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AccountBatchRepositoryImpl implements AccountBatchRepository{

    private final JdbcTemplate jdbcTemplate;

    public AccountBatchRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveAccountsBatch(List<Account> accounts) {
        String sql = """
                INSERT INTO accounts (id, owner, created_at, amount, currency)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """;

        jdbcTemplate.batchUpdate(sql, accounts, 500, (ps, acc) -> {
            ps.setObject(1, acc.getId());
            ps.setObject(2, acc.getOwner());
            ps.setObject(3, acc.getCreatedAt());
            ps.setBigDecimal(4, acc.getBalance().getAmount());
            ps.setString(5, acc.getBalance().getCurrency().getCurrencyCode());
        });
    }
}

package com.github.lucasyukio.caseitauautorizador.repository;

import com.github.lucasyukio.caseitauautorizador.model.Account;
import com.github.lucasyukio.caseitauautorizador.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class AccountRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("accountsdb")
            .withUsername("user")
            .withPassword("password")
            .withInitScript("init-tests.sql");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDb() {
        jdbcTemplate.execute("DELETE FROM accounts");
    }

    @Test
    void shouldInsertAccountsInBatch() {
        Account acc1 = new Account(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now(),
                new Money(BigDecimal.ZERO, Currency.getInstance("BRL"))
        );

        Account acc2 = new Account(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now(),
                new Money(BigDecimal.ZERO, Currency.getInstance("BRL"))
        );

        accountRepository.saveAccountsBatch(List.of(acc1, acc2));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM accounts");
        assertThat(rows).hasSize(2);
    }

    @Test
    void shouldNotInsertDuplicates() {
        UUID accountId = UUID.randomUUID();

        Account acc1 = new Account(
                accountId,
                UUID.randomUUID(),
                LocalDateTime.now(),
                new Money(BigDecimal.ZERO, Currency.getInstance("BRL"))
        );

        Account acc2 = new Account(
                accountId,
                UUID.randomUUID(),
                LocalDateTime.now(),
                new Money(BigDecimal.ZERO, Currency.getInstance("BRL"))
        );

        accountRepository.saveAccountsBatch(List.of(acc1));
        accountRepository.saveAccountsBatch(List.of(acc2));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM accounts");
        assertThat(rows).hasSize(1);
    }
}

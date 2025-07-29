package com.github.lucasyukio.caseitauautorizador.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.lucasyukio.caseitauautorizador.dto.request.TransactionRequest;
import com.github.lucasyukio.caseitauautorizador.model.Account;
import com.github.lucasyukio.caseitauautorizador.model.Money;
import com.github.lucasyukio.caseitauautorizador.repository.AccountRepository;
import com.github.lucasyukio.caseitauautorizador.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

import static com.github.lucasyukio.caseitauautorizador.model.enums.TransactionType.CREDIT;
import static com.github.lucasyukio.caseitauautorizador.model.enums.TransactionType.DEBIT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-tests.sql");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private UUID accountId;

    private final Currency currency = Currency.getInstance("BRL");

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        accountId = UUID.randomUUID();
        Account account = new Account(accountId, UUID.randomUUID(), LocalDateTime.now(), new Money(BigDecimal.valueOf(100), currency));
        accountRepository.save(account);
    }

    @Test
    void shouldCreateCreditTransactionSuccessfully() throws Exception {
        TransactionRequest request = new TransactionRequest(accountId, CREDIT, BigDecimal.valueOf(50), currency);

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.account.balance.amount").value(150.00));
    }

    @Test
    void shouldCreateDebitTransactionSuccessfully() throws Exception {
        TransactionRequest request = new TransactionRequest(accountId, DEBIT, BigDecimal.valueOf(50), currency);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEBIT"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.account.balance.amount").value(50.00));
    }

    @Test
    void shouldFailTransactionDueToInsufficientFunds() throws Exception {
        TransactionRequest request = new TransactionRequest(accountId, DEBIT, BigDecimal.valueOf(200), currency);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.account.balance.amount").value(100.00));
    }

    @Test
    void shouldReturnErrorWhenAccountNotFound() throws Exception {
        TransactionRequest request = new TransactionRequest(UUID.randomUUID(), CREDIT, BigDecimal.valueOf(50), currency);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }
}

package com.github.lucasyukio.caseitauautorizador.service;

import com.github.lucasyukio.caseitauautorizador.dto.request.TransactionRequest;
import com.github.lucasyukio.caseitauautorizador.dto.response.TransactionResponse;
import com.github.lucasyukio.caseitauautorizador.model.Account;
import com.github.lucasyukio.caseitauautorizador.model.Money;
import com.github.lucasyukio.caseitauautorizador.model.Transaction;
import com.github.lucasyukio.caseitauautorizador.repository.AccountRepository;
import com.github.lucasyukio.caseitauautorizador.repository.TransactionRepository;
import com.github.lucasyukio.caseitauautorizador.service.impl.TransactionServiceImpl;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static com.github.lucasyukio.caseitauautorizador.model.enums.TransactionStatus.FAILED;
import static com.github.lucasyukio.caseitauautorizador.model.enums.TransactionType.CREDIT;
import static com.github.lucasyukio.caseitauautorizador.model.enums.TransactionType.DEBIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private TransactionServiceImpl transactionService;

    private final Currency currency = Currency.getInstance("BRL");
    private final UUID accountId = UUID.randomUUID();
    private final Account account = new Account(
            accountId,
            UUID.randomUUID(),
            LocalDateTime.now(),
            new Money(BigDecimal.valueOf(100), currency)
    );

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        transactionService = new TransactionServiceImpl(accountRepository, transactionRepository);
    }

    @Test
    void shouldCreateCreditTransactionSuccessfully() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        TransactionRequest request = new TransactionRequest(accountId, CREDIT, BigDecimal.valueOf(50), currency);
        TransactionResponse response = transactionService.createTransaction(request);

        assertEquals(CREDIT, response.type());
        assertEquals(BigDecimal.valueOf(150), response.account().balance().amount());

        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(account);
    }

    @Test
    void shouldCreateDebitTransactionSuccessfully() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        TransactionRequest request = new TransactionRequest(accountId, DEBIT, BigDecimal.valueOf(50), currency);
        TransactionResponse response = transactionService.createTransaction(request);

        assertEquals(DEBIT, response.type());
        assertEquals(BigDecimal.valueOf(50), response.account().balance().amount());

        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(account);
    }

    @Test
    void shouldFailTransactionDueToInsufficientFunds() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        TransactionRequest request = new TransactionRequest(accountId, DEBIT, BigDecimal.valueOf(200), currency);
        TransactionResponse response = transactionService.createTransaction(request);

        assertEquals(DEBIT, response.type());
        assertEquals(FAILED, response.status());

        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldFailTransactionAfterMaxRetriesDueToOptimisticLock() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        doThrow(new OptimisticLockException()).when(accountRepository).save(any(Account.class));

        TransactionRequest request = new TransactionRequest(accountId, CREDIT, BigDecimal.valueOf(50), currency);
        TransactionResponse response = transactionService.createTransaction(request);

        assertEquals(FAILED, response.status());
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        TransactionRequest request = new TransactionRequest(accountId, CREDIT, BigDecimal.valueOf(50), currency);

        assertThrows(IllegalArgumentException.class, () -> transactionService.createTransaction(request));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}

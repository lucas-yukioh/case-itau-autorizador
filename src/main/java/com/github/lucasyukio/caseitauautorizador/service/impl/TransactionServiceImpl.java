package com.github.lucasyukio.caseitauautorizador.service.impl;

import com.github.lucasyukio.caseitauautorizador.dto.request.TransactionRequest;
import com.github.lucasyukio.caseitauautorizador.dto.response.AccountBalanceResponse;
import com.github.lucasyukio.caseitauautorizador.dto.response.AccountResponse;
import com.github.lucasyukio.caseitauautorizador.dto.response.TransactionAmountResponse;
import com.github.lucasyukio.caseitauautorizador.dto.response.TransactionResponse;
import com.github.lucasyukio.caseitauautorizador.model.Account;
import com.github.lucasyukio.caseitauautorizador.model.Money;
import com.github.lucasyukio.caseitauautorizador.model.Transaction;
import com.github.lucasyukio.caseitauautorizador.model.enums.TransactionStatus;
import com.github.lucasyukio.caseitauautorizador.model.enums.TransactionType;
import com.github.lucasyukio.caseitauautorizador.repository.AccountRepository;
import com.github.lucasyukio.caseitauautorizador.repository.TransactionRepository;
import com.github.lucasyukio.caseitauautorizador.service.TransactionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final Counter successfulTransactions;
    private final Counter failedTransactions;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Value("${transaction.max-retries}")
    private int maxRetries;

    public TransactionServiceImpl(AccountRepository accountRepository, TransactionRepository transactionRepository, MeterRegistry meterRegistry) {
        this.successfulTransactions = meterRegistry.counter("transactions_success_total");
        this.failedTransactions = meterRegistry.counter("transactions_failed_total");

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        int attempts = 0;

        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        while (true) {
            try {
                return executeTransaction(request, account);
            } catch (OptimisticLockException e) {
                if (++attempts >= maxRetries) {
                    LOGGER.error("Transaction {} FAILED after {} retries due to concurrent updates (account {})", request.type(), attempts, account.getId());
                    return createAndSaveTransaction(request, TransactionStatus.FAILED, account);
                }
            }
        }
    }

    private TransactionResponse executeTransaction(TransactionRequest request, Account account) {
        if (isDebitWithInsufficientFunds(request, account)) {
            LOGGER.error("Transaction {} FAILED due to insufficient funds. Account ID: {}, Current Balance: {}, Requested: {}", request.type(), account.getId(), account.getBalance().getAmount(), request.value());
            return createAndSaveTransaction(request, TransactionStatus.FAILED, account);
        }

        updateAccountBalance(account, request);
        return createAndSaveTransaction(request, TransactionStatus.SUCCEEDED, account);
    }

    private boolean isDebitWithInsufficientFunds(TransactionRequest request, Account account) {
        return request.type() == TransactionType.DEBIT &&
                account.getBalance().getAmount().compareTo(request.value()) < 0;
    }

    private void updateAccountBalance(Account account, TransactionRequest request) {
        Money balance = account.getBalance();
        var newAmount = request.type() == TransactionType.CREDIT
                ? balance.getAmount().add(request.value())
                : balance.getAmount().subtract(request.value());

        account.setBalance(new Money(newAmount, balance.getCurrency()));
        accountRepository.save(account);
    }

    private TransactionResponse createAndSaveTransaction(TransactionRequest request, TransactionStatus status, Account account) {
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                request.type(),
                new Money(request.value(), request.currency()),
                status,
                LocalDateTime.now(),
                account
        );

        transactionRepository.save(transaction);

        incrementCounter(status);

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                new TransactionAmountResponse(transaction.getValue().getAmount(), transaction.getValue().getCurrency()),
                transaction.getStatus(),
                transaction.getTransactionDate(),
                new AccountResponse(account.getId(), new AccountBalanceResponse(account.getBalance().getAmount(), account.getBalance().getCurrency()))
        );
    }

    private void incrementCounter(TransactionStatus status) {
        if (TransactionStatus.SUCCEEDED == status) {
            successfulTransactions.increment();
        } else {
            failedTransactions.increment();
        }
    }
}

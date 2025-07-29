package com.github.lucasyukio.caseitauautorizador.model;

import com.github.lucasyukio.caseitauautorizador.model.enums.TransactionStatus;
import com.github.lucasyukio.caseitauautorizador.model.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Embedded
    private Money value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account;

    public Transaction() {}

    public Transaction(UUID id, TransactionType type, Money value, TransactionStatus status, LocalDateTime transactionDate, Account account) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.status = status;
        this.transactionDate = transactionDate;
        this.account = account;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public Money getValue() { return value; }
    public void setValue(Money value) { this.value = value; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}

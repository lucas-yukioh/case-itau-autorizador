package com.github.lucasyukio.caseitauautorizador.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Embedded
    private Balance balance;

    public Account() {
    }

    public Account(UUID id, UUID owner, LocalDateTime createdAt, Balance balance) {
        this.id = id;
        this.owner = owner;
        this.createdAt = createdAt;
        this.balance = balance;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Balance getBalance() { return balance; }
    public void setBalance(Balance balance) { this.balance = balance; }
}

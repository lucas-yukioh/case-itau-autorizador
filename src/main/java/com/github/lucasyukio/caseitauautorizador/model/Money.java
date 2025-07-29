package com.github.lucasyukio.caseitauautorizador.model;

import com.github.lucasyukio.caseitauautorizador.model.utils.CurrencyConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Currency;

@Embeddable
public class Money {

    private BigDecimal amount;

    @Convert(converter = CurrencyConverter.class)
    private Currency currency;

    public Money() {
    }

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
}

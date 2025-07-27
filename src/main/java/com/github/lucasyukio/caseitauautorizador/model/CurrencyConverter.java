package com.github.lucasyukio.caseitauautorizador.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Currency;

@Converter(autoApply = true)
public class CurrencyConverter implements AttributeConverter<Currency, String> {

    @Override
    public String convertToDatabaseColumn(Currency currency) {
        return (currency != null) ? currency.getCurrencyCode() : null;
    }

    @Override
    public Currency convertToEntityAttribute(String dbCurrency) {
        return (dbCurrency != null) ? Currency.getInstance(dbCurrency) : null;
    }
}

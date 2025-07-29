package com.github.lucasyukio.caseitauautorizador.model;

import com.github.lucasyukio.caseitauautorizador.model.utils.CurrencyConverter;
import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyConverterTest {

    private final CurrencyConverter converter = new CurrencyConverter();

    @Test
    void shouldConvertCurrencyToString() {
        String dbValue = converter.convertToDatabaseColumn(Currency.getInstance("BRL"));
        assertEquals("BRL", dbValue);
    }

    @Test
    void shouldConvertStringToCurrency() {
        Currency currency = converter.convertToEntityAttribute("BRL");
        assertEquals("BRL", currency.getCurrencyCode());
    }

    @Test
    void shouldReturnNullWhenValueIsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void shouldThrowExceptionForInvalidCurrency() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute("INVALID"));
    }
}

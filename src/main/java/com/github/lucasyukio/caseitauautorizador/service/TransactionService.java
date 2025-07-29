package com.github.lucasyukio.caseitauautorizador.service;

import com.github.lucasyukio.caseitauautorizador.dto.request.TransactionRequest;
import com.github.lucasyukio.caseitauautorizador.dto.response.TransactionResponse;

public interface TransactionService {

    TransactionResponse createTransaction(TransactionRequest request);
}

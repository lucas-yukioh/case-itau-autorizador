package com.github.lucasyukio.caseitauautorizador.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.lucasyukio.caseitauautorizador.dto.AccountMessage;
import com.github.lucasyukio.caseitauautorizador.dto.OuterMessage;
import com.github.lucasyukio.caseitauautorizador.model.Account;
import com.github.lucasyukio.caseitauautorizador.model.Balance;
import com.github.lucasyukio.caseitauautorizador.repository.AccountRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SqsBatchConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsBatchConsumer.class);

    private final SqsAsyncClient sqsAsyncClient;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private static final int POLLER_COUNT = 5;
    private volatile boolean running = true;

    @Value("${aws.sqs.queueUrl}")
    private String queueUrl;

    public SqsBatchConsumer(SqsAsyncClient sqsAsyncClient, AccountRepository accountRepository, ObjectMapper objectMapper) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.accountRepository = accountRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void startPolling() {
        for (int i = 0; i < POLLER_COUNT; i++) {
            pollMessages();
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;

        executorService.shutdownNow();
        sqsAsyncClient.close();
    }

    private void pollMessages() {
        if (!running) return;

        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)
                .build();

        sqsAsyncClient.receiveMessage(request)
                .thenAccept(response -> {
                    if (!response.messages().isEmpty()) {
                        executorService.submit(() -> processBatch(response.messages()));
                    }
                    pollMessages();
                })
                .exceptionally(ex -> {
                    LOGGER.error("Error polling SQS: {}", ex.getMessage());
                    pollMessages();
                    return null;
                });
    }

    private void processBatch(List<Message> messages) {
        try {
            List<Account> accounts = messages.stream().map(msg -> {
                try {
                    OuterMessage outerMessage = objectMapper.readValue(msg.body(), OuterMessage.class);
                    AccountMessage accountMessage = outerMessage.account();

                    LocalDateTime createdAt = Instant.ofEpochSecond(Long.parseLong(accountMessage.createdAt()))
                            .atZone(ZoneOffset.UTC)
                            .toLocalDateTime();

                    return new Account(
                            accountMessage.id(),
                            accountMessage.owner(),
                            createdAt,
                            new Balance(BigDecimal.ZERO, Currency.getInstance("BRL"))
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Invalid Message: " + msg.body(), e);
                }
            }).toList();

            accountRepository.saveAccountsBatch(accounts);
            deleteMessages(messages);
        } catch (Exception e) {
            LOGGER.error("Failed to process batch: {}", e.getMessage());
        }
    }

    private void deleteMessages(List<Message> messages) {
        DeleteMessageBatchRequest deleteRequest = DeleteMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(messages.stream()
                        .map(msg -> DeleteMessageBatchRequestEntry.builder()
                                .id(msg.messageId())
                                .receiptHandle(msg.receiptHandle())
                                .build())
                        .toList())
                .build();

        sqsAsyncClient.deleteMessageBatch(deleteRequest);
    }
}

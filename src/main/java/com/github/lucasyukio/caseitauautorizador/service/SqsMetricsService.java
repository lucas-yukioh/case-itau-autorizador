package com.github.lucasyukio.caseitauautorizador.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class SqsMetricsService {

    private final SqsAsyncClient sqsAsyncClient;

    @Value("${aws.sqs.queueUrl}")
    private String queueUrl;

    private final AtomicReference<Double> messagesVisible = new AtomicReference<>(0.0);
    private final AtomicReference<Double> messagesInflight = new AtomicReference<>(0.0);
    private final AtomicReference<Double> messagesDelayed = new AtomicReference<>(0.0);

    public SqsMetricsService(SqsAsyncClient sqsAsyncClient, MeterRegistry meterRegistry) {
        this.sqsAsyncClient = sqsAsyncClient;

        Gauge.builder("sqs_messages_visible", messagesVisible, AtomicReference::get)
                .description("Number of messages available in SQS")
                .register(meterRegistry);

        Gauge.builder("sqs_messages_inflight", messagesInflight, AtomicReference::get)
                .description("Number of messages in flight (being processed)")
                .register(meterRegistry);

        Gauge.builder("sqs_messages_delayed", messagesDelayed, AtomicReference::get)
                .description("Number of delayed messages in SQS")
                .register(meterRegistry);
    }

    @PostConstruct
    @Scheduled(fixedRate = 10000)
    public void updateMetrics() {
        GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED
                )
                .build();

        sqsAsyncClient.getQueueAttributes(request)
                .thenAccept(response -> {
                    var attributes = response.attributes();
                    messagesVisible.set(parse(attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)));
                    messagesInflight.set(parse(attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)));
                    messagesDelayed.set(parse(attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED)));
                })
                .exceptionally(ex -> {
                    messagesVisible.set(0.0);
                    messagesInflight.set(0.0);
                    messagesDelayed.set(0.0);
                    return null;
                });
    }

    private double parse(String value) {
        try {
            return value == null ? 0.0 : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}

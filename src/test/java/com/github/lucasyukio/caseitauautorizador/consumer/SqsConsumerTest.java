package com.github.lucasyukio.caseitauautorizador.consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class SqsConsumerTest {

    static String queueUrl;
    static SqsAsyncClient sqsAsyncClient;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("accountsdb")
            .withUsername("user")
            .withPassword("password")
            .withInitScript("init-tests.sql");

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.7.2"))
            .withServices(LocalStackContainer.Service.SQS)
            .withStartupTimeout(Duration.ofSeconds(60));

    @BeforeAll
    static void setupQueue() throws Exception {
        sqsAsyncClient = SqsAsyncClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
                .region(Region.SA_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();

        queueUrl = sqsAsyncClient.createQueue(CreateQueueRequest.builder()
                .queueName("test-queue")
                .build()).get(5, TimeUnit.SECONDS).queueUrl();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);

        registry.add("aws.sqs.endpoint",
                () -> localstack.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
        registry.add("aws.sqs.queueUrl", () -> queueUrl);

        registry.add("sqs.poller.count", () -> 1);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDb() {
        jdbcTemplate.execute("DELETE FROM accounts");
    }

    @Test
    void shouldProcessMessageAndInsertIntoDatabase() throws Exception {
        String messageBody = """
                {"account":{"id":"11111111-1111-1111-1111-111111111111",
                "owner":"22222222-2222-2222-2222-222222222222",
                "created_at":"1730865931","status":"ENABLED"}}
                """;

        sqsAsyncClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build()).get(5, TimeUnit.SECONDS);

        Thread.sleep(3000);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM accounts");
        assertFalse(rows.isEmpty());
    }

    @Test
    void shouldSkipInvalidJsonMessages() throws Exception {
        sqsAsyncClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody("{\"invalid-json\":}")
                .build()).get();

        Thread.sleep(3000);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM accounts");
        assertTrue(rows.isEmpty());
    }

    @Test
    void shouldInsertMessageOnlyOnceIfDuplicate() throws Exception {
        String messageBody = """
                {"account":{"id":"11111111-1111-1111-1111-111111111111",
                "owner":"22222222-2222-2222-2222-222222222222",
                "created_at":"1730865931","status":"ENABLED"}}
                """;

        sqsAsyncClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build()).get();
        sqsAsyncClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build()).get();

        Thread.sleep(4000);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM accounts");
        assertEquals(1, rows.size());
    }
}

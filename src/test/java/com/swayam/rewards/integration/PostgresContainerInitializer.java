package com.swayam.rewards.integration;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers PostgreSQL container.
 * One container is started once and reused across all integration test classes
 * via the static field, keeping the test suite fast.
 */
public class PostgresContainerInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.4")
                    .withDatabaseName("rewardsdb_test")
                    .withUsername("test_user")
                    .withPassword("test_pass");

    static {
        POSTGRES.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "spring.datasource.username=" + POSTGRES.getUsername(),
                "spring.datasource.password=" + POSTGRES.getPassword()
        ).applyTo(ctx.getEnvironment());
    }
}

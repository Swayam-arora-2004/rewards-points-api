package com.swayam.rewards.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Concurrency test: 20 simultaneous redeem calls of 10 points on a 100-point balance.
 * Expected result: exactly 10 succeed (200), 10 fail (409), final balance = 0.
 *
 * The @RepeatedTest(5) annotation runs this 5 times as required by Project_Context.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresContainerInitializer.class)
class ConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long memberId;

    @BeforeEach
    void setUpMemberWith100Points() throws Exception {
        String email = "concurrent_" + System.nanoTime() + "@example.com";
        String createBody = objectMapper.writeValueAsString(Map.of("name", "ConcUser", "email", email));

        String response = mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        memberId = objectMapper.readTree(response).get("id").asLong();

        // Earn 100 points
        mockMvc.perform(post("/members/" + memberId + "/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 100))))
                .andExpect(status().isOk());
    }

    @RepeatedTest(5)
    void concurrentRedeem_20threads_exactly10SucceedAnd10Fail() throws Exception {
        int threads = 20;
        int pointsPerRedeem = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        String redeemBody = objectMapper.writeValueAsString(Map.of("points", pointsPerRedeem));
        final Long id = memberId;

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                MvcResult result = mockMvc.perform(post("/members/" + id + "/redeem")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(redeemBody))
                        .andReturn();
                return result.getResponse().getStatus();
            });
        }

        List<Future<Integer>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        long successes = futures.stream().map(f -> {
            try { return f.get(); } catch (Exception e) { return 0; }
        }).filter(s -> s == 200).count();

        long failures = futures.stream().map(f -> {
            try { return f.get(); } catch (Exception e) { return 0; }
        }).filter(s -> s == 409).count();

        // Verify exactly 10 succeeded and 10 failed
        assertThat(successes).as("successful redeems").isEqualTo(10);
        assertThat(failures).as("rejected redeems (insufficient balance)").isEqualTo(10);

        // Verify final balance is 0
        String memberJson = mockMvc.perform(get("/members/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long finalBalance = objectMapper.readTree(memberJson).get("balance").asLong();
        assertThat(finalBalance).as("final balance after concurrent redeems").isEqualTo(0L);
    }
}


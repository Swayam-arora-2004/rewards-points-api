package com.swayam.rewards.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests through the full HTTP stack using a real PostgreSQL container.
 * Tests verify: create member, earn, redeem (OK), redeem (insufficient balance), 404.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresContainerInitializer.class)
class MemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long memberId;

    @BeforeEach
    void createMember() throws Exception {
        // Create a fresh member before each test – email is unique per test via timestamp
        String email = "alice+" + System.currentTimeMillis() + "@example.com";
        String body = objectMapper.writeValueAsString(Map.of("name", "Alice", "email", email));

        String response = mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        memberId = objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void earn_validRequest_returns200WithNewBalance() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("points", 100));

        mockMvc.perform(post("/members/" + memberId + "/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100));
    }

    @Test
    void redeem_sufficientBalance_returns200() throws Exception {
        // First earn 100 points
        mockMvc.perform(post("/members/" + memberId + "/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 100))))
                .andExpect(status().isOk());

        // Then redeem 30
        mockMvc.perform(post("/members/" + memberId + "/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70));
    }

    @Test
    void redeem_insufficientBalance_returns409() throws Exception {
        // No points earned – balance is 0
        mockMvc.perform(post("/members/" + memberId + "/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 50))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getMember_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/members/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMember_knownId_returnsHistoryNewestFirst() throws Exception {
        // earn 100, then redeem 20 – history should be REDEEM then EARN
        mockMvc.perform(post("/members/" + memberId + "/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 100))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/members/" + memberId + "/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 20))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/members/" + memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(80))
                .andExpect(jsonPath("$.transactions[0].type").value("REDEEM"))
                .andExpect(jsonPath("$.transactions[1].type").value("EARN"));
    }

    @Test
    void createMember_duplicateEmail_returns409() throws Exception {
        // The @BeforeEach created a member; creating with the same email should fail
        // We need to know the email – create another member with a known email
        String email = "dupe_" + System.currentTimeMillis() + "@example.com";
        String body = objectMapper.writeValueAsString(Map.of("name", "Bob", "email", email));

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Second create with same email
        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void earn_invalidPoints_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("points", -1));

        mockMvc.perform(post("/members/" + memberId + "/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

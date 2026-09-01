package com.patechltd.Salexfy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SyncApiTests {

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(empty())))
                .andReturn().getResponse().getContentAsString();
        token = response.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    void pushThenPullRoundTrip() throws Exception {
        String push = "{\"deviceId\":\"device-a\",\"changes\":["
                + "{\"seq\":1,\"entityType\":\"PRODUCT\",\"recordId\":\"p-1\","
                + "\"operation\":\"INSERT\",\"payload\":\"{\\\"uid\\\":\\\"p-1\\\"}\",\"updatedAt\":1},"
                + "{\"seq\":2,\"entityType\":\"UNIT\",\"recordId\":\"u-1\","
                + "\"operation\":\"INSERT\",\"payload\":\"{\\\"uid\\\":\\\"u-1\\\"}\",\"updatedAt\":2}"
                + "]}";

        mockMvc.perform(post("/api/sync/push")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(push))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged", hasSize(2)));

        // The same device must not receive its own records back.
        mockMvc.perform(get("/api/sync/pull")
                        .header("Authorization", "Bearer " + token)
                        .param("since", "0")
                        .param("deviceId", "device-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changes", hasSize(0)));

        // Another device sees them.
        mockMvc.perform(get("/api/sync/pull")
                        .header("Authorization", "Bearer " + token)
                        .param("since", "0")
                        .param("deviceId", "device-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changes", hasSize(2)));
    }

    @Test
    void unauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/sync/pull").param("since", "0"))
                .andExpect(status().isForbidden());
    }
}

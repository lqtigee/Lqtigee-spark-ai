package com.lqtigee.sparkai.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "lqtigee.security.api-token=test-token")
@AutoConfigureMockMvc
class ModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listModelsWithValidTokenReturnsJsonList() throws Exception {
        mockMvc.perform(get("/api/models")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.models").isArray())
                .andExpect(jsonPath("$.models[0].id").value("gpt-5.5"))
                .andExpect(jsonPath("$.models[0].label").value("GPT-5.5"))
                .andExpect(jsonPath("$.models[0].commandModelName").value("gpt-5.5"))
                .andExpect(jsonPath("$.models[0].sources[0]").value("CODEX"))
                .andExpect(jsonPath("$.models[0].enabled").value(true))
                .andExpect(jsonPath("$.models[1].id").value("openai/Lqtigee"))
                .andExpect(jsonPath("$.models[1].label").value("Lqtigee"))
                .andExpect(jsonPath("$.models[1].commandModelName").value("openai/Lqtigee"))
                .andExpect(jsonPath("$.models[1].sources[0]").value("OPENCODE"))
                .andExpect(jsonPath("$.models[1].enabled").value(true));
    }

    @Test
    void listModelsWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/models"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));
    }
}

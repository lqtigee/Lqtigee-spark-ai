package com.lqtigee.sparkai.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lqtigee.sparkai.dto.CodexSkillDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.service.CodexSkillService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "lqtigee.security.api-token=test-token")
@AutoConfigureMockMvc
class CodexSkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodexSkillService codexSkillService;

    @Test
    void listSkillsWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/codex/skills"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));

        verifyNoInteractions(codexSkillService);
    }

    @Test
    void listSkillsWithValidTokenReturnsRealServiceSkills() throws Exception {
        when(codexSkillService.listSkills()).thenReturn(List.of(
                new CodexSkillDto(
                        "openai-docs|/home/lqtiger/.codex/skills/.system/openai-docs/SKILL.md",
                        "openai-docs",
                        "/home/lqtiger/.codex/skills/.system/openai-docs/SKILL.md",
                        "Use when Codex needs official OpenAI documentation."
                )
        ));

        mockMvc.perform(get("/api/codex/skills")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codexSkills[0].id").value("openai-docs|/home/lqtiger/.codex/skills/.system/openai-docs/SKILL.md"))
                .andExpect(jsonPath("$.codexSkills[0].name").value("openai-docs"))
                .andExpect(jsonPath("$.codexSkills[0].sourcePath").value("/home/lqtiger/.codex/skills/.system/openai-docs/SKILL.md"))
                .andExpect(jsonPath("$.codexSkills[0].description").value("Use when Codex needs official OpenAI documentation."));

        verify(codexSkillService).listSkills();
    }

    @Test
    void listSkillsServiceFailureReturnsTypedError() throws Exception {
        when(codexSkillService.listSkills()).thenThrow(new ApiException(
                ErrorCode.CODEX_SESSION_SCAN_FAILED,
                HttpStatus.FAILED_DEPENDENCY,
                "Codex skill scan failed",
                "permission denied"
        ));

        mockMvc.perform(get("/api/codex/skills")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isFailedDependency())
                .andExpect(jsonPath("$.code").value("CODEX_SESSION_SCAN_FAILED"))
                .andExpect(jsonPath("$.detail").value("permission denied"));

        verify(codexSkillService).listSkills();
    }
}

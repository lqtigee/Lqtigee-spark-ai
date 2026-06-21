package com.lqtigee.sparkai.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lqtigee.sparkai.adapter.CodexAdapter;
import com.lqtigee.sparkai.adapter.OpencodeAdapter;
import com.lqtigee.sparkai.dto.AdapterHealthDto;
import com.lqtigee.sparkai.dto.AgentSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodexAdapter codexAdapter;

    @MockitoBean
    private OpencodeAdapter opencodeAdapter;

    @Test
    void healthReturnsOkWhenBothAdaptersAreAvailable() throws Exception {
        when(codexAdapter.probe()).thenReturn(available(AgentSource.CODEX, "codex-cli 0.141.0"));
        when(opencodeAdapter.probe()).thenReturn(available(AgentSource.OPENCODE, "1.17.8"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("Lqtigee-spark-ai"))
                .andExpect(jsonPath("$.appName").value("Lqtigee"))
                .andExpect(jsonPath("$.port").value(20261))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.adapters[0].source").value("CODEX"))
                .andExpect(jsonPath("$.adapters[0].available").value(true))
                .andExpect(jsonPath("$.adapters[0].status").value("OK"))
                .andExpect(jsonPath("$.adapters[0].version").value("codex-cli 0.141.0"))
                .andExpect(jsonPath("$.adapters[0].lastErrorCode").doesNotExist())
                .andExpect(jsonPath("$.adapters[0].lastErrorMessage").doesNotExist())
                .andExpect(jsonPath("$.adapters[1].source").value("OPENCODE"))
                .andExpect(jsonPath("$.adapters[1].available").value(true))
                .andExpect(jsonPath("$.adapters[1].status").value("OK"))
                .andExpect(jsonPath("$.adapters[1].version").value("1.17.8"));
    }

    @Test
    void healthReturnsDegradedWhenOneAdapterIsUnavailable() throws Exception {
        when(codexAdapter.probe()).thenReturn(available(AgentSource.CODEX, "codex-cli 0.141.0"));
        when(opencodeAdapter.probe()).thenReturn(unavailable(AgentSource.OPENCODE, "OPENCODE_BIN_NOT_FOUND"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.adapters[0].available").value(true))
                .andExpect(jsonPath("$.adapters[1].available").value(false))
                .andExpect(jsonPath("$.adapters[1].lastErrorCode").value("OPENCODE_BIN_NOT_FOUND"));
    }

    @Test
    void healthReturnsFailedWhenNoAdapterIsAvailable() throws Exception {
        when(codexAdapter.probe()).thenReturn(unavailable(AgentSource.CODEX, "CODEX_BIN_NOT_FOUND"));
        when(opencodeAdapter.probe()).thenReturn(unavailable(AgentSource.OPENCODE, "OPENCODE_BIN_NOT_FOUND"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.adapters[0].available").value(false))
                .andExpect(jsonPath("$.adapters[1].available").value(false));
    }

    private AdapterHealthDto available(AgentSource source, String version) {
        return new AdapterHealthDto(source, true, "OK", version, null, null);
    }

    private AdapterHealthDto unavailable(AgentSource source, String errorCode) {
        return new AdapterHealthDto(source, false, "UNAVAILABLE", null, errorCode, "adapter unavailable");
    }
}

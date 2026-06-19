package com.lqtigee.sparkai.config;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;

@ConfigurationProperties(prefix = "lqtigee.models")
public class ModelProperties {

    private List<Entry> entries = new ArrayList<>();

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public void validate() {
        if (entries == null || entries.isEmpty()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "Model configuration is empty",
                    "lqtigee.models.entries"
            );
        }

        Set<String> modelIds = new HashSet<>();
        for (Entry entry : entries) {
            String id = entry == null ? null : entry.getId();
            if (id != null && !modelIds.add(id)) {
                throw new ApiException(
                        ErrorCode.VALIDATION_FAILED,
                        HttpStatus.BAD_REQUEST,
                        "Duplicate model id",
                        id
                );
            }
        }
    }

    public static class Entry {

        private String id;
        private String label;
        private String commandModelName;
        private List<AgentSource> sources = new ArrayList<>();
        private boolean enabled;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getCommandModelName() {
            return commandModelName;
        }

        public void setCommandModelName(String commandModelName) {
            this.commandModelName = commandModelName;
        }

        public List<AgentSource> getSources() {
            return sources;
        }

        public void setSources(List<AgentSource> sources) {
            this.sources = sources;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

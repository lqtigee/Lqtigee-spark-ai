package com.lqtigee.sparkai.config;

import com.lqtigee.sparkai.dto.AgentSource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lqtigee.models")
public class ModelProperties {

    private List<Entry> entries = new ArrayList<>();

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
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

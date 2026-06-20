package com.lqtigee.sparkai.config;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;

@ConfigurationProperties(prefix = "lqtigee.remote")
public class RemoteProperties {

    private int maxPromptChars;

    public int getMaxPromptChars() {
        return maxPromptChars;
    }

    public void setMaxPromptChars(int maxPromptChars) {
        this.maxPromptChars = maxPromptChars;
    }

    public void validate() {
        if (maxPromptChars <= 0) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "Remote prompt limit must be positive",
                    "lqtigee.remote.max-prompt-chars"
            );
        }
    }
}

package com.lqtigee.sparkai.config;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;

@ConfigurationProperties(prefix = "lqtigee.security")
public class SecurityProperties {

    private String apiToken;

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public void validate() {
        if (apiToken == null || apiToken.isBlank()) {
            throw new ApiException(
                    ErrorCode.AUTH_TOKEN_MISSING,
                    HttpStatus.UNAUTHORIZED,
                    "API token is required",
                    "lqtigee.security.api-token"
            );
        }
    }
}

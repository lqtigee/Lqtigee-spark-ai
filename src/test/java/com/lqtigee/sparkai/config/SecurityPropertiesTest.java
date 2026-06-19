package com.lqtigee.sparkai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

    @Test
    void validateFailsWhenApiTokenIsEmpty() {
        SecurityProperties properties = new SecurityProperties();
        properties.setApiToken("");

        assertThatThrownBy(properties::validate)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.AUTH_TOKEN_MISSING));
    }

    @Test
    void validatePassesWhenApiTokenIsPresent() {
        SecurityProperties properties = new SecurityProperties();
        properties.setApiToken("test-token");

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }
}

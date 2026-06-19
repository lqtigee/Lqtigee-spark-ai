package com.lqtigee.sparkai.config;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.security.BearerTokenFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<Filter> bearerTokenFilterRegistration(
            SecurityProperties securityProperties,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        BearerTokenFilter bearerTokenFilter = new BearerTokenFilter(securityProperties);
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setName("bearerTokenFilter");
        registration.setFilter((request, response, chain) -> {
            try {
                bearerTokenFilter.doFilter(request, response, chain);
            } catch (ApiException exception) {
                handlerExceptionResolver.resolveException(
                        (HttpServletRequest) request,
                        (HttpServletResponse) response,
                        null,
                        exception
                );
            }
        });
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}

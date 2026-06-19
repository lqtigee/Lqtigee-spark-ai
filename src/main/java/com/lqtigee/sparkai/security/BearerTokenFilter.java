package com.lqtigee.sparkai.security;

import com.lqtigee.sparkai.config.SecurityProperties;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class BearerTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecurityProperties securityProperties;

    public BearerTokenFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/health".equals(path) || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        securityProperties.validate();

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            throw new ApiException(
                    ErrorCode.AUTH_TOKEN_MISSING,
                    HttpStatus.UNAUTHORIZED,
                    "Authorization bearer token is required",
                    null
            );
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw invalidToken();
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (!securityProperties.getApiToken().equals(token)) {
            throw invalidToken();
        }

        filterChain.doFilter(request, response);
    }

    private ApiException invalidToken() {
        return new ApiException(
                ErrorCode.AUTH_TOKEN_INVALID,
                HttpStatus.UNAUTHORIZED,
                "Authorization bearer token is invalid",
                null
        );
    }
}

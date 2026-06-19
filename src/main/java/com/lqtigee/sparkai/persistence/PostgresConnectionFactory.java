package com.lqtigee.sparkai.persistence;

import com.lqtigee.sparkai.config.DatabaseProperties;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.http.HttpStatus;

public class PostgresConnectionFactory {

    private final DatabaseProperties databaseProperties;

    public PostgresConnectionFactory(DatabaseProperties databaseProperties) {
        this.databaseProperties = databaseProperties;
    }

    public Connection open() {
        if (!databaseProperties.isEnabled()) {
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.FAILED_DEPENDENCY,
                    "PostgreSQL persistence is disabled",
                    "lqtigee.database.enabled"
            );
        }
        requirePresent(databaseProperties.getUrl(), "lqtigee.database.url");
        requirePresent(databaseProperties.getUsername(), "lqtigee.database.username");
        requirePresent(databaseProperties.getPassword(), "lqtigee.database.password");

        try {
            return DriverManager.getConnection(
                    databaseProperties.getUrl(),
                    databaseProperties.getUsername(),
                    databaseProperties.getPassword()
            );
        } catch (SQLException exception) {
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.FAILED_DEPENDENCY,
                    "PostgreSQL connection failed",
                    exception.getMessage()
            );
        }
    }

    private void requirePresent(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "PostgreSQL configuration is missing",
                    propertyName
            );
        }
    }
}

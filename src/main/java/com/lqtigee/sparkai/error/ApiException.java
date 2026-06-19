package com.lqtigee.sparkai.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;
    private final String detail;

    public ApiException(ErrorCode code, HttpStatus status, String message, String detail) {
        super(message);
        this.code = code;
        this.status = status;
        this.detail = detail;
    }

    public ErrorCode code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String detail() {
        return detail;
    }
}

package com.trendbite.menuprofit.dto.response;

public class ErrorResponseDto {

    private final boolean success = false;
    private final String  message;

    public ErrorResponseDto(String message) {
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String  getMessage() { return message; }
}

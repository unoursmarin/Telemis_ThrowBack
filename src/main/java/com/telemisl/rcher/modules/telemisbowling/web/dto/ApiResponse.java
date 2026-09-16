package com.telemisl.rcher.modules.telemisbowling.web.dto;

/** Standard get envelop response. */
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

}

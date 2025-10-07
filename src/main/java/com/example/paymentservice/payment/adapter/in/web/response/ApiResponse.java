package com.example.paymentservice.payment.adapter.in.web.response;

import org.springframework.http.HttpStatus;

/**
 * <h4> 응답을 표준화 하기위한 객체 </h4>
 */
public record ApiResponse<T> (

    int status,
    String message,
    T data

) {

    public static <T> ApiResponse<T> with(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status.value(), message, data);
    }
}

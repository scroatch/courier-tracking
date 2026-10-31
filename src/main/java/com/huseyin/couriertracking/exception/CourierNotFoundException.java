package com.huseyin.couriertracking.exception;

public class CourierNotFoundException extends RuntimeException {
    public CourierNotFoundException(String courierId) {

        super(courierId + " not found");
    }
}

package com.biodatamaker.exception;

import lombok.Getter;

/**
 * Exception thrown when payment is required to access a resource.
 */
@Getter
public class PaymentRequiredException extends RuntimeException {

    private final Long bioDataId;

    public PaymentRequiredException(String message, Long bioDataId) {
        super(message);
        this.bioDataId = bioDataId;
    }

    public PaymentRequiredException(Long bioDataId) {
        super("Payment required to download this bio-data");
        this.bioDataId = bioDataId;
    }
}

package com.checkout.payment.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;

public record FieldError(
    @Schema(description = "Request field (JSON name) that failed validation.",
        example = "card_number")
    String field,

    @Schema(example = "must be 14-19 digits")
    String message) {
}
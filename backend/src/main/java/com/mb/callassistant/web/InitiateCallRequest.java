package com.mb.callassistant.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /api/calls/initiate}. Input is validated at the boundary
 * (phase guardrails): the phone number is constrained to the formats a dialer
 * accepts, and tnbCode is a short alphanumeric branch code.
 */
public record InitiateCallRequest(

        @NotBlank(message = "phoneNumber is required")
        @Pattern(regexp = "^[A-Za-z0-9_.+*#@:-]{1,128}$",
                message = "phoneNumber contains invalid characters")
        String phoneNumber,

        @NotBlank(message = "tnbCode is required")
        @Size(max = 32, message = "tnbCode must be at most 32 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$",
                message = "tnbCode must be alphanumeric")
        String tnbCode
) {
}

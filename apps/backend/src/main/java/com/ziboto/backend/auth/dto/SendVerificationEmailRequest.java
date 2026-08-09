package com.ziboto.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to (re-)send an email verification OTP to the given address")
public class SendVerificationEmailRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "Email address to send the verification OTP to",
            example = "user@example.com")
    private String email;
}

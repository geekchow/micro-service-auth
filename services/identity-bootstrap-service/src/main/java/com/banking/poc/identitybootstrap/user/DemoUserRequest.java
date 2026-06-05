package com.banking.poc.identitybootstrap.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DemoUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String role,
        @NotBlank String customerId,
        @NotEmpty List<@NotBlank String> accountIds) {
}

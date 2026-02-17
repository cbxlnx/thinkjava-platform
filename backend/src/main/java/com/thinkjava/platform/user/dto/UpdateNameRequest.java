package com.thinkjava.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNameRequest(
    @NotBlank @Size(min = 1, max = 30) String firstName
) {}
package com.carbonlink.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRequestDTO(
        @NotBlank(message = "name is required") String name,

        @NotBlank(message = "companyName is required") String companyName,

        @NotBlank(message = "role is required") String role,

        @NotBlank(message = "city is required") String city
) {
}

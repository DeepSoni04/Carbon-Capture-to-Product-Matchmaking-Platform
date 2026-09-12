package com.carbonlink.dto;

import com.carbonlink.entity.Role;

import java.time.LocalDateTime;

public record UserResponseDTO(
        Long id,
        String name,
        String companyName,
        Role role,
        Double locationLat,
        Double locationLng,
        LocalDateTime createdAt
) {
}

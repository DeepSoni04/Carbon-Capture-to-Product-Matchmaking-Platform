package com.carbonlink.dto;

import com.carbonlink.entity.ListingStatus;

import java.time.LocalDateTime;

public record ListingResponseDTO(
        Long id,
        Long emitterId,
        Double totalVolumeTons,
        Double remainingVolumeTons,
        Double purityPercent,
        String captureMethod,
        Double pricePerTon,
        Double locationLat,
        Double locationLng,
        ListingStatus status,
        LocalDateTime createdAt
) {
}

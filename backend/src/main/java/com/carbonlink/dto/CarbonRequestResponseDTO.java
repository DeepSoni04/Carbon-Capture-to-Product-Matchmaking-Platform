package com.carbonlink.dto;

import com.carbonlink.entity.RequestStatus;

import java.time.LocalDateTime;

public record CarbonRequestResponseDTO(
        Long id,
        Long buyerId,
        Double minVolumeNeeded,
        Double minPurityRequired,
        Double maxDistanceKm,
        Double maxBudgetPerTon,
        String intendedUse,
        RequestStatus status,
        LocalDateTime createdAt
) {
}

package com.carbonlink.dto;

import com.carbonlink.entity.DistanceSource;
import com.carbonlink.entity.MatchStatus;

import java.time.LocalDateTime;

public record MatchResponseDTO(
        Long id,
        Long listingId,
        Long requestId,
        Double compatibilityScore,
        Double distanceKm,
        DistanceSource distanceSource,
        MatchStatus status,
        ScoreBreakdown scoreBreakdown,
        CostBreakdown costBreakdown,
        Double transactedVolumeTons,
        LocalDateTime createdAt
) {
}

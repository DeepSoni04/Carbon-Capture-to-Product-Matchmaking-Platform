package com.carbonlink.dto;

import com.carbonlink.entity.DistanceSource;

public record CostBreakdown(
        double basePrice,
        double transportCost,
        double totalCost,
        double distanceKm,
        DistanceSource distanceSource
) {
}

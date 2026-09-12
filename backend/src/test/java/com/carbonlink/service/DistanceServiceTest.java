package com.carbonlink.service;

import com.carbonlink.dto.DistanceResult;
import com.carbonlink.entity.DistanceSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistanceServiceTest {

    private static final String API_URL = "https://api.openrouteservice.org/v2/matrix/driving-car";
    private static final String API_KEY = "test-key";

    // Bengaluru -> Chennai, ~290km straight-line
    private static final double LAT1 = 12.9716;
    private static final double LNG1 = 77.5946;
    private static final double LAT2 = 13.0827;
    private static final double LNG2 = 80.2707;

    @Mock
    private RestTemplate restTemplate;

    private DistanceService distanceService;

    @BeforeEach
    void setUp() {
        distanceService = new DistanceService(restTemplate, API_KEY, API_URL);
    }

    @Test
    void getDistance_whenApiSucceeds_returnsOpenRouteServiceResult() {
        DistanceService.OrsMatrixResponse body = new DistanceService.OrsMatrixResponse(
                List.of(List.of(0.0, 15000.0), List.of(15000.0, 0.0)));
        when(restTemplate.postForEntity(eq(API_URL), any(HttpEntity.class), eq(DistanceService.OrsMatrixResponse.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        DistanceResult result = distanceService.getDistance(LAT1, LNG1, LAT2, LNG2);

        assertThat(result.source()).isEqualTo(DistanceSource.OPENROUTESERVICE);
        assertThat(result.distanceKm()).isEqualTo(15.0);
    }

    @Test
    void getDistance_whenApiThrowsException_fallsBackToHaversine() {
        when(restTemplate.postForEntity(eq(API_URL), any(HttpEntity.class), eq(DistanceService.OrsMatrixResponse.class)))
                .thenThrow(new RestClientException("simulated timeout"));

        DistanceResult result = distanceService.getDistance(LAT1, LNG1, LAT2, LNG2);

        double expectedKm = distanceService.haversine(LAT1, LNG1, LAT2, LNG2) * 1.3;
        assertThat(result.source()).isEqualTo(DistanceSource.HAVERSINE_FALLBACK);
        assertThat(result.distanceKm()).isCloseTo(expectedKm, within(0.001));
    }

    @Test
    void getDistance_whenApiReturnsNon2xxStatus_fallsBackToHaversine() {
        when(restTemplate.postForEntity(eq(API_URL), any(HttpEntity.class), eq(DistanceService.OrsMatrixResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        DistanceResult result = distanceService.getDistance(LAT1, LNG1, LAT2, LNG2);

        assertThat(result.source()).isEqualTo(DistanceSource.HAVERSINE_FALLBACK);
    }

    @Test
    void getDistance_repeatedCoordinatePairs_hitCacheAndCallApiOnce() {
        DistanceService.OrsMatrixResponse body = new DistanceService.OrsMatrixResponse(
                List.of(List.of(0.0, 10000.0), List.of(10000.0, 0.0)));
        when(restTemplate.postForEntity(eq(API_URL), any(HttpEntity.class), eq(DistanceService.OrsMatrixResponse.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        distanceService.getDistance(LAT1, LNG1, LAT2, LNG2);
        distanceService.getDistance(LAT1, LNG1, LAT2, LNG2);
        // Rounds to the same 4-decimal-place key as LAT1/LNG1/LAT2/LNG2 -> should still hit cache
        distanceService.getDistance(12.97160001, 77.59460001, 13.08270001, 80.27070001);

        verify(restTemplate, times(1))
                .postForEntity(eq(API_URL), any(HttpEntity.class), eq(DistanceService.OrsMatrixResponse.class));
    }

    @Test
    void haversine_knownRoute_isReasonablyAccurate() {
        double distanceKm = distanceService.haversine(LAT1, LNG1, LAT2, LNG2);
        assertThat(distanceKm).isBetween(280.0, 300.0);
    }
}

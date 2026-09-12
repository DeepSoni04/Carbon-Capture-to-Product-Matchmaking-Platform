package com.carbonlink.service;

import com.carbonlink.dto.MatchResponseDTO;
import com.carbonlink.entity.CarbonRequest;
import com.carbonlink.entity.DistanceSource;
import com.carbonlink.entity.Listing;
import com.carbonlink.entity.ListingStatus;
import com.carbonlink.entity.Match;
import com.carbonlink.entity.MatchStatus;
import com.carbonlink.entity.RequestStatus;
import com.carbonlink.exception.ConflictException;
import com.carbonlink.exception.ResourceNotFoundException;
import com.carbonlink.repository.CarbonRequestRepository;
import com.carbonlink.repository.ListingRepository;
import com.carbonlink.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private CarbonRequestRepository carbonRequestRepository;
    @Mock
    private MatchingService matchingService; // unused by accept()/reject() - only findMatches() needs it

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository, listingRepository, carbonRequestRepository,
                matchingService, new CostEstimatorService(8.0));
    }

    @Test
    void accept_reducesRemainingVolume_byTheTransactedAmount() {
        Listing listing = listing(1000.0, 1000.0);
        CarbonRequest request = request(300.0);
        Match match = match(listing.getId(), request.getId());
        stubLookups(match, listing, request);

        MatchResponseDTO response = matchService.accept(match.getId());

        assertThat(listing.getRemainingVolumeTons()).isEqualTo(700.0);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(response.transactedVolumeTons()).isEqualTo(300.0);
        assertThat(response.status()).isEqualTo(MatchStatus.ACCEPTED);
    }

    @Test
    void accept_transactedVolumeIsRequestNeed_whenRemainingStockIsPlentiful() {
        Listing listing = listing(1000.0, 1000.0);
        CarbonRequest request = request(300.0);
        Match match = match(listing.getId(), request.getId());
        stubLookups(match, listing, request);

        MatchResponseDTO response = matchService.accept(match.getId());

        assertThat(response.transactedVolumeTons()).isEqualTo(300.0);
    }

    @Test
    void accept_transactedVolumeIsRemainingStock_whenStockIsTheConstraint() {
        Listing listing = listing(1000.0, 200.0); // only 200 tons left
        CarbonRequest request = request(500.0);    // buyer wants 500
        Match match = match(listing.getId(), request.getId());
        stubLookups(match, listing, request);

        MatchResponseDTO response = matchService.accept(match.getId());

        assertThat(response.transactedVolumeTons()).isEqualTo(200.0);
        assertThat(listing.getRemainingVolumeTons()).isEqualTo(0.0);
        // Cost must reflect the 200 tons actually transacted, not the 500 the buyer originally asked for.
        assertThat(response.costBreakdown().basePrice()).isEqualTo(listing.getPricePerTon() * 200.0);
    }

    @Test
    void accept_closesListingWhenRemainingVolumeHitsExactlyZero() {
        Listing listing = listing(1000.0, 300.0);
        CarbonRequest request = request(300.0); // exactly depletes it
        Match match = match(listing.getId(), request.getId());
        stubLookups(match, listing, request);

        matchService.accept(match.getId());

        assertThat(listing.getRemainingVolumeTons()).isEqualTo(0.0);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.CLOSED);
    }

    @Test
    void accept_closesListingWhenRemainingVolumeFallsBelowOneTonThreshold() {
        Listing listing = listing(1000.0, 300.5);
        CarbonRequest request = request(300.0); // leaves 0.5 tons - under the 1-ton close threshold
        Match match = match(listing.getId(), request.getId());
        stubLookups(match, listing, request);

        matchService.accept(match.getId());

        assertThat(listing.getRemainingVolumeTons()).isEqualTo(0.5);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.CLOSED);
    }

    @Test
    void accept_leavesListingActiveWhenMeaningfulStockRemains() {
        Listing listing = listing(1000.0, 500.0);
        CarbonRequest request = request(100.0); // leaves 400 tons - well above the threshold
        Match match = match(listing.getId(), request.getId());
        stubLookups(match, listing, request);

        matchService.accept(match.getId());

        assertThat(listing.getRemainingVolumeTons()).isEqualTo(400.0);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.ACTIVE);
    }

    @Test
    void accept_setsRequestStatusToMatched() {
        Listing listing = listing(1000.0, 1000.0);
        CarbonRequest request = request(300.0);
        Match match = match(listing.getId(), request.getId());
        stubLookups(match, listing, request);

        matchService.accept(match.getId());

        assertThat(request.getStatus()).isEqualTo(RequestStatus.MATCHED);
    }

    @Test
    void accept_alreadyDecidedMatch_throwsConflict() {
        Match decided = Match.builder().id(1L).listingId(1L).requestId(1L).status(MatchStatus.ACCEPTED).build();
        when(matchRepository.findById(1L)).thenReturn(Optional.of(decided));

        assertThatThrownBy(() -> matchService.accept(1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void accept_matchNotFound_throwsResourceNotFoundException() {
        when(matchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.accept(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubLookups(Match match, Listing listing, CarbonRequest request) {
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(carbonRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(carbonRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Listing listing(double total, double remaining) {
        return Listing.builder()
                .id(1L).emitterId(10L)
                .totalVolumeTons(total)
                .remainingVolumeTons(remaining)
                .purityPercent(95.0)
                .captureMethod("Direct Air Capture")
                .pricePerTon(900.0)
                .locationLat(0.0).locationLng(0.0)
                .status(ListingStatus.ACTIVE)
                .build();
    }

    private CarbonRequest request(double minVolumeNeeded) {
        return CarbonRequest.builder()
                .id(1L)
                .buyerId(20L)
                .minVolumeNeeded(minVolumeNeeded)
                .minPurityRequired(90.0)
                .maxDistanceKm(500.0)
                .maxBudgetPerTon(1000.0)
                .intendedUse("Fuel Synthesis")
                .status(RequestStatus.OPEN)
                .build();
    }

    private Match match(Long listingId, Long requestId) {
        return Match.builder()
                .id(1L)
                .listingId(listingId)
                .requestId(requestId)
                .compatibilityScore(90.0)
                .distanceKm(50.0)
                .distanceSource(DistanceSource.HAVERSINE_FALLBACK)
                .transportCost(1000.0)
                .totalEstimatedCost(50000.0)
                .status(MatchStatus.SUGGESTED)
                .build();
    }
}

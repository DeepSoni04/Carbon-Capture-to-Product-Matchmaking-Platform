package com.carbonlink.service;

import com.carbonlink.dto.CostBreakdown;
import com.carbonlink.dto.MatchResponseDTO;
import com.carbonlink.dto.MatchResult;
import com.carbonlink.dto.ScoreBreakdown;
import com.carbonlink.entity.CarbonRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Bridges MatchingService's stateless scoring with persisted Match rows so accept/reject have an id to act on.
@Service
public class MatchService {

    private static final Set<MatchStatus> DECIDED_STATUSES = Set.of(MatchStatus.ACCEPTED, MatchStatus.REJECTED);
    private static final double MIN_REMAINING_VOLUME_TONS = 1.0;

    private final MatchRepository matchRepository;
    private final ListingRepository listingRepository;
    private final CarbonRequestRepository carbonRequestRepository;
    private final MatchingService matchingService;
    private final CostEstimatorService costEstimatorService;

    public MatchService(MatchRepository matchRepository,
                         ListingRepository listingRepository,
                         CarbonRequestRepository carbonRequestRepository,
                         MatchingService matchingService,
                         CostEstimatorService costEstimatorService) {
        this.matchRepository = matchRepository;
        this.listingRepository = listingRepository;
        this.carbonRequestRepository = carbonRequestRepository;
        this.matchingService = matchingService;
        this.costEstimatorService = costEstimatorService;
    }

    @Transactional
    public List<MatchResponseDTO> getRankedMatchesWithCost(Long requestId) {
        List<MatchResult> results = matchingService.findMatches(requestId);
        CarbonRequest request = requireRequest(requestId);

        List<MatchResponseDTO> fresh = results.stream()
                .map(result -> upsertSuggestion(result, request))
                .filter(dto -> dto != null)
                .toList();

        // MatchingService only scores currently-ACTIVE listings, so a match whose listing has
        // since become MATCHED/CLOSED (e.g. this one got accepted) would otherwise vanish from
        // the buyer's own results. Keep it visible by falling back to the persisted row.
        Set<Long> freshListingIds = fresh.stream().map(MatchResponseDTO::listingId).collect(Collectors.toSet());
        List<MatchResponseDTO> stale = matchRepository.findByRequestId(requestId).stream()
                .filter(match -> !freshListingIds.contains(match.getListingId()))
                .map(match -> toResponseDto(match, requireListing(match.getListingId()), request))
                .toList();

        List<MatchResponseDTO> combined = new ArrayList<>(fresh);
        combined.addAll(stale);
        combined.sort(Comparator.comparing(MatchResponseDTO::compatibilityScore).reversed());
        return combined;
    }

    @Transactional
    public MatchResponseDTO accept(Long matchId) {
        Match match = requireMatch(matchId);
        requireUndecided(match);

        Listing listing = requireListing(match.getListingId());
        CarbonRequest request = requireRequest(match.getRequestId());

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ConflictException("Listing " + listing.getId() + " is no longer active");
        }
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new ConflictException("Request " + request.getId() + " is no longer open");
        }

        double transactedVolumeTons = round2(Math.min(request.getMinVolumeNeeded(), listing.getRemainingVolumeTons()));
        double remainingAfter = round2(Math.max(0.0, listing.getRemainingVolumeTons() - transactedVolumeTons));

        match.setStatus(MatchStatus.ACCEPTED);
        match.setTransactedVolumeTons(transactedVolumeTons);

        listing.setRemainingVolumeTons(remainingAfter);
        listing.setStatus(remainingAfter < MIN_REMAINING_VOLUME_TONS ? ListingStatus.CLOSED : ListingStatus.ACTIVE);

        request.setStatus(RequestStatus.MATCHED);

        matchRepository.save(match);
        listingRepository.save(listing);
        carbonRequestRepository.save(request);

        return toResponseDto(match, listing, request);
    }

    @Transactional
    public MatchResponseDTO reject(Long matchId) {
        Match match = requireMatch(matchId);
        requireUndecided(match);

        Listing listing = requireListing(match.getListingId());
        CarbonRequest request = requireRequest(match.getRequestId());

        match.setStatus(MatchStatus.REJECTED);
        matchRepository.save(match);

        return toResponseDto(match, listing, request);
    }

    @Transactional
    public MatchResponseDTO request(Long matchId) {
        Match match = requireMatch(matchId);
        requireUndecided(match);

        Listing listing = requireListing(match.getListingId());
        CarbonRequest request = requireRequest(match.getRequestId());

        match.setStatus(MatchStatus.REQUESTED);
        matchRepository.save(match);

        return toResponseDto(match, listing, request);
    }

    @Transactional(readOnly = true)
    public List<MatchResponseDTO> getMatchesForListing(Long listingId) {
        Listing listing = requireListing(listingId);

        return matchRepository.findByListingId(listingId).stream()
                .map(match -> toResponseDto(match, listing, requireRequest(match.getRequestId())))
                .toList();
    }

    private MatchResponseDTO upsertSuggestion(MatchResult result, CarbonRequest request) {
        Match existing = matchRepository.findByListingIdAndRequestId(result.listingId(), result.requestId())
                .orElse(null);

        if (existing != null && DECIDED_STATUSES.contains(existing.getStatus())) {
            // Already accepted or rejected elsewhere - don't resurface it as a new suggestion.
            return null;
        }

        Listing listing = requireListing(result.listingId());

        Match match = existing != null ? existing : Match.builder()
                .listingId(result.listingId())
                .requestId(result.requestId())
                .build();

        match.setCompatibilityScore(result.compatibilityScore());
        match.setDistanceKm(result.distanceKm());
        match.setDistanceSource(result.distanceSource());

        CostBreakdown costBreakdown = costEstimatorService.estimateCost(
                listing, request.getMinVolumeNeeded(), result.distanceKm(), result.distanceSource());
        match.setTransportCost(costBreakdown.transportCost());
        match.setTotalEstimatedCost(costBreakdown.totalCost());

        match = matchRepository.save(match);

        return new MatchResponseDTO(
                match.getId(),
                match.getListingId(),
                match.getRequestId(),
                match.getCompatibilityScore(),
                match.getDistanceKm(),
                match.getDistanceSource(),
                match.getStatus(),
                result.scoreBreakdown(),
                costBreakdown,
                match.getTransactedVolumeTons(),
                match.getCreatedAt());
    }

    private MatchResponseDTO toResponseDto(Match match, Listing listing, CarbonRequest request) {
        // Once accepted, transactedVolumeTons is the real committed volume - cost should reflect
        // that, not the (possibly larger) volume the buyer originally asked for.
        double effectiveVolumeTons = match.getTransactedVolumeTons() != null
                ? match.getTransactedVolumeTons()
                : request.getMinVolumeNeeded();

        ScoreBreakdown scoreBreakdown = MatchingService.scoreListing(listing, request, match.getDistanceKm());
        CostBreakdown costBreakdown = costEstimatorService.estimateCost(
                listing, effectiveVolumeTons, match.getDistanceKm(), match.getDistanceSource());

        return new MatchResponseDTO(
                match.getId(),
                match.getListingId(),
                match.getRequestId(),
                match.getCompatibilityScore(),
                match.getDistanceKm(),
                match.getDistanceSource(),
                match.getStatus(),
                scoreBreakdown,
                costBreakdown,
                match.getTransactedVolumeTons(),
                match.getCreatedAt());
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void requireUndecided(Match match) {
        if (DECIDED_STATUSES.contains(match.getStatus())) {
            throw new ConflictException(
                    "Match " + match.getId() + " has already been " + match.getStatus().name().toLowerCase());
        }
    }

    private Match requireMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
    }

    private Listing requireListing(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));
    }

    private CarbonRequest requireRequest(Long id) {
        return carbonRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));
    }
}

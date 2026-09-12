package com.carbonlink.controller;

import com.carbonlink.dto.ListingRequestDTO;
import com.carbonlink.dto.ListingResponseDTO;
import com.carbonlink.dto.MatchResponseDTO;
import com.carbonlink.entity.ListingStatus;
import com.carbonlink.service.ListingService;
import com.carbonlink.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;
    private final MatchService matchService;

    public ListingController(ListingService listingService, MatchService matchService) {
        this.listingService = listingService;
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<ListingResponseDTO> createListing(@Valid @RequestBody ListingRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listingService.createListing(dto));
    }

    @GetMapping
    public List<ListingResponseDTO> getListings(
            @RequestParam(required = false) Double minPurity,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) ListingStatus status) {
        return listingService.getListings(minPurity, maxPrice, status);
    }

    @GetMapping("/{id}")
    public ListingResponseDTO getListing(@PathVariable Long id) {
        return listingService.getListing(id);
    }

    @GetMapping("/{id}/matches")
    public List<MatchResponseDTO> getMatches(@PathVariable Long id) {
        return matchService.getMatchesForListing(id);
    }
}

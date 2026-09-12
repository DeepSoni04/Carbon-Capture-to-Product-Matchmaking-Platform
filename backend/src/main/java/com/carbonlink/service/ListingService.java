package com.carbonlink.service;

import com.carbonlink.constants.CityCoordinates;
import com.carbonlink.dto.ListingRequestDTO;
import com.carbonlink.dto.ListingResponseDTO;
import com.carbonlink.entity.Listing;
import com.carbonlink.entity.ListingStatus;
import com.carbonlink.entity.Role;
import com.carbonlink.exception.BadRequestException;
import com.carbonlink.exception.ResourceNotFoundException;
import com.carbonlink.repository.ListingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserService userService;

    public ListingService(ListingRepository listingRepository, UserService userService) {
        this.listingRepository = listingRepository;
        this.userService = userService;
    }

    public ListingResponseDTO createListing(ListingRequestDTO dto) {
        userService.requireUserWithRole(dto.emitterId(), Role.EMITTER);
        CityCoordinates.LatLng coordinates = CityCoordinates.lookup(dto.city())
                .orElseThrow(() -> new BadRequestException("city must be one of the supported cities"));

        Listing listing = Listing.builder()
                .emitterId(dto.emitterId())
                .totalVolumeTons(dto.totalVolumeTons())
                .purityPercent(dto.purityPercent())
                .captureMethod(dto.captureMethod())
                .pricePerTon(dto.pricePerTon())
                .locationLat(coordinates.lat())
                .locationLng(coordinates.lng())
                .build();

        return toResponseDto(listingRepository.save(listing));
    }

    public ListingResponseDTO getListing(Long id) {
        return toResponseDto(requireListing(id));
    }

    public List<ListingResponseDTO> getListings(Double minPurity, Double maxPrice, ListingStatus status) {
        ListingStatus effectiveStatus = status != null ? status : ListingStatus.ACTIVE;

        return listingRepository.findByStatus(effectiveStatus).stream()
                .filter(listing -> minPurity == null || listing.getPurityPercent() >= minPurity)
                .filter(listing -> maxPrice == null || listing.getPricePerTon() <= maxPrice)
                .map(this::toResponseDto)
                .toList();
    }

    Listing requireListing(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));
    }

    private ListingResponseDTO toResponseDto(Listing listing) {
        return new ListingResponseDTO(
                listing.getId(),
                listing.getEmitterId(),
                listing.getTotalVolumeTons(),
                listing.getRemainingVolumeTons(),
                listing.getPurityPercent(),
                listing.getCaptureMethod(),
                listing.getPricePerTon(),
                listing.getLocationLat(),
                listing.getLocationLng(),
                listing.getStatus(),
                listing.getCreatedAt());
    }
}

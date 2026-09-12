package com.carbonlink.service;

import com.carbonlink.dto.CarbonRequestDTO;
import com.carbonlink.dto.CarbonRequestResponseDTO;
import com.carbonlink.entity.CarbonRequest;
import com.carbonlink.entity.Role;
import com.carbonlink.exception.ResourceNotFoundException;
import com.carbonlink.repository.CarbonRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarbonRequestService {

    private final CarbonRequestRepository carbonRequestRepository;
    private final UserService userService;

    public CarbonRequestService(CarbonRequestRepository carbonRequestRepository, UserService userService) {
        this.carbonRequestRepository = carbonRequestRepository;
        this.userService = userService;
    }

    public CarbonRequestResponseDTO createRequest(CarbonRequestDTO dto) {
        userService.requireUserWithRole(dto.buyerId(), Role.BUYER);

        CarbonRequest request = CarbonRequest.builder()
                .buyerId(dto.buyerId())
                .minVolumeNeeded(dto.minVolumeNeeded())
                .minPurityRequired(dto.minPurityRequired())
                .maxDistanceKm(dto.maxDistanceKm())
                .maxBudgetPerTon(dto.maxBudgetPerTon())
                .intendedUse(dto.intendedUse())
                .build();

        return toResponseDto(carbonRequestRepository.save(request));
    }

    public CarbonRequestResponseDTO getRequest(Long id) {
        return toResponseDto(requireRequest(id));
    }

    public List<CarbonRequestResponseDTO> getRequests(Long buyerId) {
        List<CarbonRequest> requests = buyerId != null
                ? carbonRequestRepository.findByBuyerId(buyerId)
                : carbonRequestRepository.findAll();
        return requests.stream().map(this::toResponseDto).toList();
    }

    CarbonRequest requireRequest(Long id) {
        return carbonRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));
    }

    private CarbonRequestResponseDTO toResponseDto(CarbonRequest request) {
        return new CarbonRequestResponseDTO(
                request.getId(),
                request.getBuyerId(),
                request.getMinVolumeNeeded(),
                request.getMinPurityRequired(),
                request.getMaxDistanceKm(),
                request.getMaxBudgetPerTon(),
                request.getIntendedUse(),
                request.getStatus(),
                request.getCreatedAt());
    }
}

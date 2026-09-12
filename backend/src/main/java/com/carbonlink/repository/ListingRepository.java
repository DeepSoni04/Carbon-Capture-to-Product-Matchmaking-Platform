package com.carbonlink.repository;

import com.carbonlink.entity.Listing;
import com.carbonlink.entity.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findByEmitterId(Long emitterId);
}

package com.carbonlink.repository;

import com.carbonlink.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByListingId(Long listingId);

    List<Match> findByRequestId(Long requestId);

    Optional<Match> findByListingIdAndRequestId(Long listingId, Long requestId);
}

package com.carbonlink.seed;

import com.carbonlink.entity.CarbonRequest;
import com.carbonlink.entity.Listing;
import com.carbonlink.entity.Role;
import com.carbonlink.entity.User;
import com.carbonlink.repository.CarbonRequestRepository;
import com.carbonlink.repository.ListingRepository;
import com.carbonlink.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Only active with --spring.profiles.active=demo. Request thresholds are deliberately picked
// so the resulting match-score buckets (strong/marginal/none) don't depend on real vs.
// Haversine-fallback distance - see PROGRESS.md for the reasoning per request.
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final CarbonRequestRepository carbonRequestRepository;

    public DemoDataSeeder(UserRepository userRepository,
                           ListingRepository listingRepository,
                           CarbonRequestRepository carbonRequestRepository) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.carbonRequestRepository = carbonRequestRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Demo seed skipped - database already has data");
            return;
        }

        log.info("Seeding demo data...");

        User ambuja = saveUser("Rakesh Mehta", "Ambuja Cement Plant", Role.EMITTER, 19.076, 72.877);   // Mumbai
        User tataSteel = saveUser("Sunita Rao", "Tata Steel Works", Role.EMITTER, 18.520, 73.856);      // Pune
        User jswPower = saveUser("Karthik Iyer", "JSW Power Plant", Role.EMITTER, 13.083, 80.270);      // Chennai
        User ultraTech = saveUser("Priya Shah", "UltraTech Cement", Role.EMITTER, 23.023, 72.571);      // Ahmedabad

        User greenFuel = saveUser("Anjali Nair", "GreenFuel Synthetics", Role.BUYER, 19.997, 73.789);   // Nashik
        User ecoBuild = saveUser("Vikram Singh", "EcoBuild Materials", Role.BUYER, 12.972, 77.594);      // Bengaluru
        User algaeGrow = saveUser("Meera Pillai", "AlgaeGrow Farms", Role.BUYER, 21.170, 72.831);        // Surat
        User carbonCure = saveUser("Arjun Desai", "CarbonCure Concrete", Role.BUYER, 26.144, 91.736);    // Guwahati

        saveListing(ambuja.getId(), 800, 97.0, "Post-combustion", 1200, 19.076, 72.877);
        saveListing(ambuja.getId(), 150, 99.5, "Direct Air Capture", 2200, 19.076, 72.877);
        saveListing(tataSteel.getId(), 500, 92.0, "Pre-combustion", 950, 18.520, 73.856);
        saveListing(tataSteel.getId(), 1200, 88.0, "Oxy-fuel", 850, 18.520, 73.856);
        saveListing(jswPower.getId(), 300, 95.0, "Post-combustion", 1100, 13.083, 80.270);
        saveListing(jswPower.getId(), 2000, 90.0, "Oxy-fuel", 800, 13.083, 80.270);
        saveListing(ultraTech.getId(), 600, 99.0, "Direct Air Capture", 1800, 23.023, 72.571);
        saveListing(ultraTech.getId(), 50, 85.0, "Pre-combustion", 2500, 23.023, 72.571);

        // Loose thresholds against the whole pool -> most listings clear 75+.
        saveRequest(greenFuel.getId(), 100, 85.0, 3000, 2000, "Fuel Synthesis");
        // Different but still generous thresholds -> another batch of strong (>75) matches.
        saveRequest(ecoBuild.getId(), 400, 90.0, 3000, 1500, "Building Materials");
        // High minVolume/minPurity + tight budget suppress every listing into the 50-65 band
        // (one listing - the 50t/85% one - still falls under 50 and is correctly excluded).
        saveRequest(algaeGrow.getId(), 4000, 99.0, 3000, 750, "Greenhouse");
        // Brutal thresholds on every axis (volume/purity/budget, not just distance) -> nothing
        // reaches the 50 cutoff, regardless of actual OpenRouteService vs. Haversine distance.
        saveRequest(carbonCure.getId(), 3000, 99.9, 50, 500, "Algae Farming");
        // Bonus second request for variety - not tied to a required bucket.
        saveRequest(greenFuel.getId(), 1500, 93.0, 3000, 1000, "Other");

        log.info("Demo seed complete: {} users, {} listings, {} requests",
                userRepository.count(), listingRepository.count(), carbonRequestRepository.count());
    }

    private User saveUser(String name, String companyName, Role role, double lat, double lng) {
        return userRepository.save(User.builder()
                .name(name)
                .companyName(companyName)
                .role(role)
                .locationLat(lat)
                .locationLng(lng)
                .build());
    }

    private void saveListing(Long emitterId, double volume, double purity, String captureMethod,
                              double pricePerTon, double lat, double lng) {
        listingRepository.save(Listing.builder()
                .emitterId(emitterId)
                .totalVolumeTons(volume)
                .purityPercent(purity)
                .captureMethod(captureMethod)
                .pricePerTon(pricePerTon)
                .locationLat(lat)
                .locationLng(lng)
                .build());
    }

    private void saveRequest(Long buyerId, double minVolume, double minPurity,
                              double maxDistanceKm, double maxBudget, String intendedUse) {
        carbonRequestRepository.save(CarbonRequest.builder()
                .buyerId(buyerId)
                .minVolumeNeeded(minVolume)
                .minPurityRequired(minPurity)
                .maxDistanceKm(maxDistanceKm)
                .maxBudgetPerTon(maxBudget)
                .intendedUse(intendedUse)
                .build());
    }
}

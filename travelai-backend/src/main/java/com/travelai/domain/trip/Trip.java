package com.travelai.domain.trip;

import com.travelai.domain.auth.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String destination;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "arrival_time", length = 5)
    private String arrivalTime;

    @Column(name = "departure_time", length = 5)
    private String departureTime;

    @Column(name = "arrival_location")
    private String arrivalLocation;

    @Column(name = "accommodation_address")
    private String accommodationAddress;

    @Column(name = "preferred_transport", length = 20)
    private String preferredTransport;

    @Column(name = "trip_types", length = 100)
    private String tripTypes; // stored as comma-separated, e.g. "CULTURAL,FAMILY"

    @Column(name = "budget")
    private String budget; // e.g. "500" (total €)

    @Column(name = "budget_level", length = 20)
    private String budgetLevel; // BUDGET | COMFORT | LUXURY

    public java.util.List<String> getTripTypes() {
        if (tripTypes == null || tripTypes.isBlank()) return java.util.List.of();
        return java.util.Arrays.asList(tripTypes.split(","));
    }

    public void setTripTypesList(java.util.List<String> types) {
        this.tripTypes = (types == null || types.isEmpty()) ? null : String.join(",", types);
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TripStatus status = TripStatus.DRAFT;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

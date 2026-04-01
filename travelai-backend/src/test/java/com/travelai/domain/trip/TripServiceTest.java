package com.travelai.domain.trip;

import com.travelai.domain.auth.User;
import com.travelai.domain.auth.Role;
import com.travelai.domain.trip.dto.CreateTripRequest;
import com.travelai.domain.trip.dto.TripResponse;
import com.travelai.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripService — tests unitaris")
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private TripService tripService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User buildUser(String username) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(username + "@example.com")
                .username(username)
                .passwordHash("hash")
                .role(Role.USER)
                .active(true)
                .build();
    }

    private Trip buildTrip(User owner, Visibility visibility) {
        return Trip.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .title("Test Trip")
                .destination("Barcelona")
                .visibility(visibility)
                .status(TripStatus.DRAFT)
                .build();
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTrip_defaultPrivate — la visibilitat és PRIVATE per defecte (Privacy by Default)")
    void createTrip_defaultPrivate() {
        // Given
        User owner = buildUser("owner");
        CreateTripRequest request = new CreateTripRequest(
                "Mi Viaje a Barcelona",
                "Un viaje increíble",
                "Barcelona",
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(37),
                Visibility.PRIVATE  // PRIVATE per defecte
        );

        Trip savedTrip = buildTrip(owner, Visibility.PRIVATE);
        given(tripRepository.save(any(Trip.class))).willReturn(savedTrip);
        given(ratingRepository.averageScoreByTrip(savedTrip)).willReturn(Optional.empty());

        // When
        TripResponse response = tripService.createTrip(request, owner);

        // Then
        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(tripCaptor.capture());
        Trip capturedTrip = tripCaptor.getValue();

        assertThat(capturedTrip.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("deleteTrip_softDelete — estableix deletedAt, no esborra de la BD")
    void deleteTrip_softDelete() {
        // Given
        User owner = buildUser("owner");
        Trip trip = buildTrip(owner, Visibility.PRIVATE);
        UUID tripId = trip.getId();

        given(tripRepository.findByIdAndDeletedAtNull(tripId)).willReturn(Optional.of(trip));
        given(tripRepository.save(any(Trip.class))).willReturn(trip);

        // When
        tripService.deleteTrip(tripId, owner);

        // Then
        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(tripCaptor.capture());
        Trip capturedTrip = tripCaptor.getValue();

        assertThat(capturedTrip.getDeletedAt()).isNotNull();
        // No s'ha cridat cap mètode de delete real
        verify(tripRepository).findByIdAndDeletedAtNull(tripId);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    @DisplayName("duplicateTrip_isPrivate — la còpia té visibility = PRIVATE (Privacy by Default)")
    void duplicateTrip_isPrivate() {
        // Given
        User owner = buildUser("owner");
        Trip original = buildTrip(owner, Visibility.PUBLIC);
        UUID tripId = original.getId();

        given(tripRepository.findByIdAndDeletedAtNull(tripId)).willReturn(Optional.of(original));

        Trip copiedTrip = Trip.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .title("Copia de " + original.getTitle())
                .destination(original.getDestination())
                .visibility(Visibility.PRIVATE)
                .status(TripStatus.DRAFT)
                .build();

        given(tripRepository.save(any(Trip.class))).willReturn(copiedTrip);
        given(ratingRepository.averageScoreByTrip(copiedTrip)).willReturn(Optional.empty());

        // When
        TripResponse response = tripService.duplicateTrip(tripId, owner);

        // Then
        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(tripCaptor.capture());
        Trip capturedCopy = tripCaptor.getValue();

        assertThat(capturedCopy.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(capturedCopy.getStatus()).isEqualTo(TripStatus.DRAFT);
        assertThat(capturedCopy.getTitle()).startsWith("Copia de ");
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("getTrip_notOwner_throws — llança AccessDeniedException per a no propietaris de trips privats")
    void getTrip_notOwner_throws() {
        // Given
        User owner = buildUser("owner");
        User nonOwner = buildUser("intruder");
        Trip privateTrip = buildTrip(owner, Visibility.PRIVATE);
        UUID tripId = privateTrip.getId();

        given(tripRepository.findByIdAndDeletedAtNull(tripId)).willReturn(Optional.of(privateTrip));

        // When / Then
        assertThatThrownBy(() -> tripService.getTripById(tripId, nonOwner))
                .isInstanceOf(AccessDeniedException.class);
    }
}

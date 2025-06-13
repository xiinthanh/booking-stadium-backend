package com.ouroboros.pestadiumbookingbe.repository;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(UUID sportHallId, LocalDate bookingDate, UUID timeSlotId, Status status);
    Optional<Booking> findById(UUID id);
}

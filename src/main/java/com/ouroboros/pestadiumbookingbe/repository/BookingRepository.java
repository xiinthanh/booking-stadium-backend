package com.ouroboros.pestadiumbookingbe.repository;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(UUID sportHallId, LocalDate bookingDate, UUID timeSlotId, Status status);
    Optional<Booking> findById(UUID id);

    // Use pessimistic locking to prevent race conditions
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Booking> findAndLockBySportHallIdAndBookingDateAndTimeSlotId(UUID sportHallId, LocalDate bookingDate, UUID timeSlotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Booking> findAndLockBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(UUID sportHallId, LocalDate bookingDate, UUID timeSlotId, Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Booking> findAndLockById(UUID id);

    long countByUserIdAndBookingDateAndStatus(UUID userId, LocalDate date, Status status);

    List<Booking> findByUserId(UUID userId);
}

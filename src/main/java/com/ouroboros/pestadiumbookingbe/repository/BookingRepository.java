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
    Optional<Booking> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Booking> findAndLockBySportHallIdAndBookingDateAndTimeSlotIdAndStatus(UUID sportHallId, LocalDate bookingDate, UUID timeSlotId, Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Booking> findAndLockById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    long countAndLockByUserIdAndBookingDateAndStatus(UUID userId, LocalDate date, Status status);

    List<Booking> findByUserId(UUID userId);
}

package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.BadRequestException;
import com.ouroboros.pestadiumbookingbe.exception.ForbiddenException;
import com.ouroboros.pestadiumbookingbe.model.*;
import com.ouroboros.pestadiumbookingbe.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class BookingServiceIntegrationTest {

    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;
    @Autowired ProfileRepository profileRepository;
    @Autowired SportHallRepository sportHallRepository;
    @Autowired TimeSlotRepository timeSlotRepository;
    @Autowired SportRepository sportRepository;

    UUID userId, hallId, slotId, sportId;

    @BeforeEach
    void setup() {
        // persist Sport
        Sport s = new Sport();
        s.setName("Test Sport");
        s.setActive(true);
        sportRepository.save(s);
        sportId = s.getId();
        // persist dependent entities
        Profile p = new Profile();
        p.setEmail("test@example.com");
        p.setType(ProfileType.user);
        profileRepository.save(p);
        SportHall h = new SportHall();
        h.setSportId(sportId);
        h.setName("Test Hall");
        h.setLocation(SportHallLocation.indoor);
        h.setCapacity(10);
        sportHallRepository.save(h);
        TimeSlot t = new TimeSlot();
        t.setStartTime(LocalTime.of(9, 0));
        t.setEndTime(LocalTime.of(10, 0));
        t.setDurationMinutes(60);
        t.setActive(true);
        timeSlotRepository.save(t);
        userId = p.getId(); hallId = h.getId(); slotId = t.getId();
    }

    @Test
    void createBooking_persistsToDb() {
        long before = bookingRepository.count();
        Booking b = bookingService.createBooking(
            userId, hallId, sportId, LocalDate.now().plusDays(1), slotId, "purpose"
        );
        assertNotNull(b.getId());
        assertEquals(before + 1, bookingRepository.count());
        assertEquals(Status.pending, b.getStatus());
    }

    @Test
    void confirmBooking_changesStatus() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, LocalDate.now().plusDays(1), slotId, "purpose");
        Booking c = bookingService.confirmBooking(b.getId(), userId);
        assertEquals(Status.confirmed, c.getStatus());
    }

    @Test
    void createBooking_invalidUser_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(UUID.randomUUID(), hallId, sportId,
                LocalDate.now().plusDays(1), slotId, "purpose")
        );
    }

    @Test
    void createBooking_pastDate_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(userId, hallId, sportId,
                LocalDate.now().minusDays(1), slotId, "purpose")
        );
    }

    @Test
    void createBooking_emptyPurpose_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(userId, hallId, sportId,
                LocalDate.now().plusDays(1), slotId, "")
        );
    }

    @Test
    void createBooking_quotaExceeded_throwsForbidden() {
        LocalDate d = LocalDate.now().plusDays(2);
        bookingService.createBooking(userId, hallId, sportId, d, slotId, "purpose");
        assertThrows(ForbiddenException.class, () ->
            bookingService.createBooking(userId, hallId, sportId, d, slotId, "other")
        );
    }

    @Test
    void createBooking_conflictingBooking_throwsBadRequest() {
        LocalDate d = LocalDate.now().plusDays(3);
        bookingService.createBooking(userId, hallId, sportId, d, slotId, "purpose");
        // another user tries same slot
        Profile other = new Profile(); other.setEmail("other@test.com"); other.setType(ProfileType.user);
        UUID otherId = profileRepository.save(other).getId();
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(otherId, hallId, sportId, d, slotId, "purpose")
        );
    }

    @Test
    void cancelBooking_and_confirmBooking_edgeCases() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            LocalDate.now().plusDays(4), slotId, "purpose");
        Booking canceled = bookingService.cancelBooking(p.getId(), userId);
        assertEquals(Status.rejected, canceled.getStatus());
        assertNotNull(canceled.getCanceledAt());
        // cancel nonexistent
        assertThrows(BadRequestException.class, () ->
            bookingService.cancelBooking(UUID.randomUUID(), userId)
        );
    }

    @Test
    void modifyBooking_validAndInvalid() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            LocalDate.now().plusDays(5), slotId, "purpose");
        LocalDate newDate = LocalDate.now().plusDays(6);
        Booking updated = bookingService.modifyBooking(
            m.getId(), userId, userId, hallId, sportId, newDate, slotId, "newpur");
        assertEquals(newDate, updated.getBookingDate());
        assertEquals("newpur", updated.getPurpose());
        // invalid modify
        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(null, userId, userId, hallId, sportId,
                newDate, slotId, "x")
        );
    }

    @Test
    void deleteBooking_validBooking_removesFromDb() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            LocalDate.now().plusDays(7), slotId, "purpose");
        assertNotNull(bookingRepository.findById(p.getId()).orElse(null));

        // Should not throw and should remove from DB
        assertDoesNotThrow(() -> bookingService.deleteBooking(p.getId(), userId));
        assertNull(bookingRepository.findById(p.getId()).orElse(null));
    }

    @Test
    void deleteBooking_invalidBookingId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.deleteBooking(UUID.randomUUID(), userId)
        );
    }

    @Test
    void deleteBooking_invalidUserId_throwsBadRequest() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            LocalDate.now().plusDays(8), slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.deleteBooking(p.getId(), UUID.randomUUID())
        );
    }

    @Test
    void getBookingById_validId_returnsBooking() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            LocalDate.now().plusDays(9), slotId, "purpose");

        Booking found = bookingService.getBookingById(p.getId());
        assertNotNull(found);
        assertEquals(p.getId(), found.getId());
        assertEquals("purpose", found.getPurpose());
    }

    @Test
    void getBookingById_invalidId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.getBookingById(UUID.randomUUID())
        );
    }

    @Test
    void getAllBookings_returnsAllBookings() {
        // Clear any existing bookings first
        bookingRepository.deleteAll();

        // Create several bookings
        bookingService.createBooking(userId, hallId, sportId,
            LocalDate.now().plusDays(10), slotId, "first");
        bookingService.createBooking(userId, hallId, sportId,
            LocalDate.now().plusDays(11), slotId, "second");

        List<Booking> bookings = bookingService.getAllBookings();
        assertEquals(2, bookings.size());
    }

    @Test
    void getBookingsByUserId_existingUser_returnsUserBookings() {
        // Clear any existing bookings
        bookingRepository.deleteAll();

        // Create booking for our test user
        bookingService.createBooking(userId, hallId, sportId,
            LocalDate.now().plusDays(12), slotId, "mine");

        // Create second user and booking for them
        Profile other = new Profile();
        other.setEmail("other2@test.com");
        other.setType(ProfileType.user);
        UUID otherId = profileRepository.save(other).getId();

        bookingService.createBooking(otherId, hallId, sportId,
            LocalDate.now().plusDays(13), slotId, "theirs");

        // User should only see their own booking
        List<Booking> userBookings = bookingService.getBookingsByUserId(userId);
        assertEquals(1, userBookings.size());
        assertEquals("mine", userBookings.get(0).getPurpose());

        // Other user should only see their booking
        List<Booking> otherBookings = bookingService.getBookingsByUserId(otherId);
        assertEquals(1, otherBookings.size());
        assertEquals("theirs", otherBookings.get(0).getPurpose());
    }

    @Test
    void getBookingsByUserId_invalidUser_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.getBookingsByUserId(UUID.randomUUID())
        );
    }
}

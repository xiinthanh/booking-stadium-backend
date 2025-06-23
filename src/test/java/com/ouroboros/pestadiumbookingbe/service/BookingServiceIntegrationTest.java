package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.*;
import com.ouroboros.pestadiumbookingbe.model.*;
import com.ouroboros.pestadiumbookingbe.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class BookingServiceIntegrationTest {

    @Autowired BookingService bookingService;
    @MockitoSpyBean BookingRepository bookingRepository;
    @MockitoSpyBean ProfileRepository profileRepository;
    @MockitoSpyBean SportHallRepository sportHallRepository;
    @MockitoSpyBean TimeSlotRepository timeSlotRepository;
    @MockitoSpyBean SportRepository sportRepository;

    UUID userId, hallId, slotId, sportId;
    UUID otherUserId, otherHallId, otherSlotId, otherSportId;
    LocalDate date, otherDate;
    UUID adminId;

    @BeforeEach
    void setup() {
        // persist Sport
        Sport s = new Sport();
        s.setName("Test Sport");
        s.setActive(true);
        sportRepository.save(s);
        sportId = s.getId();

        Sport otherS = new Sport();
        otherS.setName("Other Sport");
        otherS.setActive(true);
        sportRepository.save(otherS);
        otherSportId = otherS.getId();

        // persist profile
        Profile p = new Profile();
        p.setEmail("test@example.com");
        p.setType(ProfileType.user);
        profileRepository.save(p);
        userId = p.getId();

        Profile otherP = new Profile();
        otherP.setEmail("other@example.com");
        otherP.setType(ProfileType.user);
        profileRepository.save(otherP);
        otherUserId = otherP.getId();

        Profile admin = new Profile();
        admin.setEmail("admin@example.com");
        admin.setType(ProfileType.admin);
        profileRepository.save(admin);
        adminId = admin.getId();

        // persist sport hall
        SportHall h = new SportHall();
        h.setSportId(sportId);
        h.setName("Test Hall");
        h.setLocation(SportHallLocation.indoor);
        h.setCapacity(10);
        sportHallRepository.save(h);
        hallId = h.getId();

        SportHall otherH = new SportHall();
        otherH.setSportId(otherSportId);
        otherH.setName("Other Hall");
        otherH.setLocation(SportHallLocation.outdoor);
        otherH.setCapacity(20);
        sportHallRepository.save(otherH);
        otherHallId = otherH.getId();

        // persist time slot
        TimeSlot t = new TimeSlot();
        t.setStartTime(LocalTime.of(9, 0));
        t.setEndTime(LocalTime.of(10, 0));
        t.setDurationMinutes(60);
        t.setActive(true);
        timeSlotRepository.save(t);
        slotId = t.getId();

        TimeSlot otherT = new TimeSlot();
        otherT.setStartTime(LocalTime.of(10, 0));
        otherT.setEndTime(LocalTime.of(11, 0));
        otherT.setDurationMinutes(60);
        otherT.setActive(true);
        timeSlotRepository.save(otherT);
        otherSlotId = otherT.getId();

        // Set dates for testing
        date = LocalDate.now().plusDays(1);
        otherDate = LocalDate.now().plusDays(2);
    }


    @Test
    void createBooking_persistsToDb() {
        long before = bookingRepository.count();

        Booking b = bookingService.createBooking(
                userId, hallId, sportId, date, slotId, "purpose"
        );

        assertNotNull(b.getId());

        assertEquals(userId, b.getUserId());
        assertEquals(hallId, b.getSportHallId());
        assertEquals(sportId, b.getSportId());
        assertEquals(date, b.getBookingDate());
        assertEquals(slotId, b.getTimeSlotId());
        assertEquals(Status.pending, b.getStatus());

        assertEquals(before + 1, bookingRepository.count());
    }

    @Test
    void createBooking_invalidUser_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(UUID.randomUUID(), hallId, sportId,
                date, slotId, "purpose")
        );
    }

    @Test
    void createBooking_invalidHall_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(userId, UUID.randomUUID(), sportId,
                date, slotId, "purpose")
        );
    }

    @Test
    void createBooking_invalidTimeSlot_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(userId, hallId, sportId,
                date, UUID.randomUUID(), "purpose")
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
    void createBooking_moreThanOneYearInFuture_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(userId, hallId, sportId,
                LocalDate.now().plusYears(1).plusDays(1), slotId, "purpose")
        );
    }

    @Test
    void createBooking_emptyPurpose_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(userId, hallId, sportId,
                date, slotId, "")
        );
    }

    @Test
    void createBooking_nullPurpose_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(userId, hallId, sportId,
                date, slotId, null)
        );
    }

    @Test
    void createBooking_quotaExceeded_throwsForbidden() {
        bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose");

        assertThrows(ForbiddenException.class, () ->
            bookingService.createBooking(userId, hallId, sportId, date, slotId, "other")
        );
    }

    @Test
    void createBooking_adminUnlimitedQuota() {
        bookingService.createBooking(adminId, hallId, sportId, date, slotId, "admin purpose");
        assertDoesNotThrow(() ->
                bookingService.createBooking(adminId, otherHallId, otherSportId, date, otherSlotId, "admin other purpose")
        );
    }

    @Test
    void createBooking_isOccupiedPendingBooking_throwsConflict() {
        bookingService.createBooking(userId, hallId, sportId, date, slotId, "original");

        // another user tries same slot
        assertThrows(ConflictException.class, () ->
            bookingService.createBooking(otherUserId, hallId, sportId, date, slotId, "conflict")
        );
    }

    @Test
    void createBooking_isOccupiedConfirmedBooking_throwsConflict() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, slotId, "original");
        bookingService.confirmBooking(b.getId(), userId); // confirm the original booking

        // another user tries same slot
        assertThrows(ConflictException.class, () ->
            bookingService.createBooking(otherUserId, hallId, sportId, date, slotId, "conflict")
        );
    }

    @Test
    void createBooking_dataAccessException_throwsServiceUnavailable() {
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(profileRepository).findById(any(UUID.class));

        assertThrows(ServiceUnavailableException.class, () ->
                bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose")
        );
    }

    @Test
    void createBooking_transactionTimeout_throwsRequestTimeout() {
        // Simulate a transaction timeout by throwing an exception
        doThrow(new TransactionTimedOutException("Transaction timed out"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RequestTimeoutException.class, () ->
                bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose")
        );
    }

    @Test
    void createBooking_genericException_throwsRuntimeException() {
        // Simulate a generic exception
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RuntimeException.class, () ->
                bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose")
        );
    }

    @Test
    void confirmBooking_pendingBooking() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose");
        Booking c = bookingService.confirmBooking(b.getId(), userId);
        assertEquals(Status.confirmed, c.getStatus());
    }
    @Test
    void confirmBooking_canceledBooking() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose");
        bookingService.cancelBooking(b.getId(), userId); // cancel first

        Booking c = bookingService.confirmBooking(b.getId(), userId);

        assertEquals(Status.confirmed, c.getStatus());
    }

    @Test
    void confirmBooking_invalidUser_throwsBadRequest() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose");
        assertThrows(BadRequestException.class, () ->
            bookingService.confirmBooking(b.getId(), UUID.randomUUID())
        );
    }

    @Test
    void confirmBooking_nonexistentBooking_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.confirmBooking(UUID.randomUUID(), userId)
        );
    }

    @Test
    void confirmBooking_alreadyConfirmed_throwsBadRequest() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose");
        bookingService.confirmBooking(b.getId(), userId); // confirm first time

        assertThrows(BadRequestException.class, () ->
            bookingService.confirmBooking(b.getId(), userId) // try to confirm again
        );
    }

    @Test
    void confirmBooking_dataAccessException_throwsServiceUnavailable() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose");
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(profileRepository).findById(userId);

        assertThrows(ServiceUnavailableException.class, () ->
            bookingService.confirmBooking(b.getId(), userId)
        );
    }

    @Test
    void confirmBooking_transactionTimeout_throwsRequestTimeout() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose");
        doThrow(new TransactionTimedOutException("Transaction timed out"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RequestTimeoutException.class, () ->
            bookingService.confirmBooking(b.getId(), userId)
        );
    }

    @Test
    void confirmBooking_genericException_throwsRuntimeException() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, slotId, "purpose");
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RuntimeException.class, () ->
            bookingService.confirmBooking(b.getId(), userId)
        );
    }

    @Test
    void cancelBooking_cancelPending() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
                date, slotId, "purpose");

        Booking canceled = bookingService.cancelBooking(p.getId(), userId);

        assertEquals(Status.rejected, canceled.getStatus());
        assertNotNull(canceled.getCanceledAt());
        assertEquals(canceled.getCanceledBy(), userId);
    }

    @Test
    void cancelBooking_cancelConfirmed() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
                date, slotId, "purpose");
        bookingService.confirmBooking(p.getId(), userId); // confirm first

        Booking canceled = bookingService.cancelBooking(p.getId(), userId);

        assertEquals(Status.rejected, canceled.getStatus());
        assertNotNull(canceled.getCanceledAt());
        assertEquals(canceled.getCanceledBy(), userId);
    }

    @Test
    void cancelBooking_nonexistentBooking_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                bookingService.cancelBooking(UUID.randomUUID(), userId)
        );
    }

    @Test
    void cancelBooking_invalidUser_throwsBadRequest() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        assertThrows(BadRequestException.class, () ->
            bookingService.cancelBooking(p.getId(), UUID.randomUUID())
        );
    }

    @Test
    void cancelBooking_alreadyCanceled_throwsBadRequest() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        bookingService.cancelBooking(p.getId(), userId); // cancel first

        assertThrows(BadRequestException.class, () ->
            bookingService.cancelBooking(p.getId(), userId) // try to cancel again
        );
    }

    @Test
    void cancelBooking_dataAccessResourceFailureException_throwsServiceUnavailable() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(profileRepository).findById(userId);

        assertThrows(ServiceUnavailableException.class, () ->
                bookingService.cancelBooking(p.getId(), userId)
        );
    }

    @Test
    void cancelBooking_transactionTimeout_throwsRequestTimeout() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        doThrow(new TransactionTimedOutException("Transaction timed out"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RequestTimeoutException.class, () ->
            bookingService.cancelBooking(p.getId(), userId)
        );
    }

    @Test
    void cancelBooking_genericException_throwsRuntimeException() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RuntimeException.class, () ->
            bookingService.cancelBooking(p.getId(), userId)
        );
    }

    @Test
    void modifyBooking_changePurpose() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
                date, slotId, "purpose");

        Booking updated = bookingService.modifyBooking(
            m.getId(), userId, userId, hallId, sportId, date, slotId, "new purpose");

        assertEquals("new purpose", updated.getPurpose());
    }

    @Test
    void modifyBooking_changeDate() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
                date, slotId, "purpose");

        Booking updated = bookingService.modifyBooking(
            m.getId(), userId, userId, hallId, sportId, otherDate, slotId, "purpose");

        assertEquals(otherDate, updated.getBookingDate());
    }

    @Test
    void modifyBooking_changeTimeSlot() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
                date, slotId, "purpose");

        Booking updated = bookingService.modifyBooking(
            m.getId(), userId, userId, hallId, sportId, date, otherSlotId, "purpose");

        assertEquals(otherSlotId, updated.getTimeSlotId());
    }

    @Test
    void modifyBooking_changeSportHall() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
                date, slotId, "purpose");

        Booking updated = bookingService.modifyBooking(
            m.getId(), userId, userId, otherHallId, sportId, date, slotId, "purpose");

        assertEquals(otherHallId, updated.getSportHallId());
    }

    @Test
    void modifyBooking_changeAll() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
                date, slotId, "purpose");

        Booking updated = bookingService.modifyBooking(
                m.getId(), otherUserId, userId, otherHallId, otherSportId, otherDate, otherSlotId, "new purpose");

        assertEquals(otherHallId, updated.getSportHallId());
        assertEquals(otherSportId, updated.getSportId());
        assertEquals(otherDate, updated.getBookingDate());
        assertEquals(otherSlotId, updated.getTimeSlotId());
        assertEquals("new purpose", updated.getPurpose());
        assertNull(updated.getCanceledAt());
        assertNull(updated.getCanceledBy());
        assertEquals(Status.pending, updated.getStatus());
    }

    @Test
    void modifyBooking_changeConfirmedBooking() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        bookingService.confirmBooking(m.getId(), userId); // confirm first

        Booking updated = bookingService.modifyBooking(
            m.getId(), userId, userId, otherHallId, otherSportId, otherDate, otherSlotId, "new purpose");

        assertEquals("new purpose", updated.getPurpose());
        assertEquals(Status.pending, updated.getStatus());
    }

    @Test
    void modifyBooking_nullBookingId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(null, userId, userId, hallId, sportId,
                date, slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_nullModifiedByUserId_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), null, userId, hallId, sportId, date, slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_invalidUser_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, UUID.randomUUID(), hallId, sportId, date, slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_invalidModifiedByUserId_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), UUID.randomUUID(), userId, hallId, sportId, date, slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_invalidSportHall_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, UUID.randomUUID(), sportId, date, slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_invalidTimeSlot_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, UUID.randomUUID(), "new purpose")
        );
    }

    @Test
    void modifyBooking_pastDate_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId,
                LocalDate.now().minusDays(1), slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_moreThanOneYearInFuture_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId,
                LocalDate.now().plusYears(1).plusDays(1), slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_emptyPurpose_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, slotId, "")
        );
    }

    @Test
    void modifyBooking_nullPurpose_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, slotId, null)
        );
    }

    @Test
    void modifyBooking_nonexistentBooking_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(UUID.randomUUID(), userId, userId, hallId, sportId, date, slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_rejectedBooking_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        bookingService.cancelBooking(m.getId(), userId); // cancel first

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_quotaExceeded_throwsForbidden() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        // Create another booking to fill quota
        bookingService.createBooking(userId, otherHallId, otherSportId,
            otherDate, otherSlotId, "other purpose");

        // Now modifying should throw ForbiddenException
        assertThrows(ForbiddenException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, otherDate, slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_occupiedPendingBooking_throwsConflict() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        bookingService.createBooking(otherUserId, otherHallId, otherSportId,
            otherDate, otherSlotId, "other purpose");

        // Another user tries to modify same slot
        assertThrows(ConflictException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, otherHallId, otherSportId,
                    otherDate, otherSlotId, "conflict purpose")
        );
    }

    @Test
    void modifyBooking_occupiedConfirmedBooking_throwsConflict() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");

        Booking mOther = bookingService.createBooking(otherUserId, otherHallId, otherSportId,
                otherDate, otherSlotId, "other purpose");
        bookingService.confirmBooking(mOther.getId(), otherUserId); // confirm first

        // Another user tries to modify same slot
        assertThrows(ConflictException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, otherHallId, otherSportId,
                    otherDate, otherSlotId, "conflict purpose")
        );
    }

    @Test
    void modifyBooking_dataAccessResourceFailureException_throwsServiceUnavailable() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(profileRepository).findById(any(UUID.class));

        assertThrows(ServiceUnavailableException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_transactionTimeout_throwsRequestTimeout() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        doThrow(new TransactionTimedOutException("Transaction timed out"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RequestTimeoutException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, slotId, "new purpose")
        );
    }

    @Test
    void modifyBooking_genericException_throwsRuntimeException() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RuntimeException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, slotId, "new purpose")
        );
    }

    @Test
    void deleteBooking_validBooking_removesFromDb() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
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
            date, slotId, "purpose");

        assertThrows(BadRequestException.class, () ->
            bookingService.deleteBooking(p.getId(), UUID.randomUUID())
        );
    }

    @Test
    void deleteBooking_dataAccessResourceFailureException_throwsServiceUnavailable() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(bookingRepository).delete(any(Booking.class));

        assertThrows(ServiceUnavailableException.class, () ->
            bookingService.deleteBooking(p.getId(), userId)
        );
    }

    @Test
    void deleteBooking_transactionTimeout_throwsRequestTimeout() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        doThrow(new TransactionTimedOutException("Transaction timed out"))
                .when(bookingRepository).delete(any(Booking.class));

        assertThrows(RequestTimeoutException.class, () ->
            bookingService.deleteBooking(p.getId(), userId)
        );
    }

    @Test
    void deleteBooking_genericException_throwsRuntimeException() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "purpose");
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).delete(any(Booking.class));

        assertThrows(RuntimeException.class, () ->
            bookingService.deleteBooking(p.getId(), userId)
        );
    }


    @Test
    void getAllBookings_returnsAllBookings() {
        // Clear any existing bookings first
        bookingRepository.deleteAll();

        // Create several bookings
        bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "first");
        bookingService.createBooking(otherUserId, otherHallId, otherSportId,
            otherDate, otherSlotId, "second");

        List<Booking> bookings = bookingService.getAllBookings();
        assertEquals(2, bookings.size());
    }

    @Test
    void getAllBookings_dataAccessResourceFailureException_throwsServiceUnavailable() {
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(bookingRepository).findAll();

        assertThrows(ServiceUnavailableException.class, () ->
            bookingService.getAllBookings()
        );
    }

    @Test
    void getAllBookings_genericException_throwsRuntimeException() {
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).findAll();

        assertThrows(RuntimeException.class, () ->
            bookingService.getAllBookings()
        );
    }

    @Test
    void getBookingById_validId_returnsBooking() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
                date, slotId, "purpose");

        Booking found = bookingService.getBookingById(p.getId());
        assertNotNull(found);
        assertEquals(p.getId(), found.getId());
    }

    @Test
    void getBookingById_invalidId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.getBookingById(UUID.randomUUID())
        );
    }

    @Test
    void getBookingById_dataAccessResourceFailureException_throwsServiceUnavailable() {
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(bookingRepository).findById(any(UUID.class));

        assertThrows(ServiceUnavailableException.class, () ->
            bookingService.getBookingById(UUID.randomUUID())
        );
    }

    @Test
    void getBookingById_genericException_throwsRuntimeException() {
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).findById(any(UUID.class));

        assertThrows(RuntimeException.class, () ->
            bookingService.getBookingById(UUID.randomUUID())
        );
    }

    @Test
    void getBookingsByUserId_existingUser_returnsUserBookings() {
        // Clear any existing bookings
        bookingRepository.deleteAll();

        // Create booking for our test user
        bookingService.createBooking(userId, hallId, sportId,
            date, slotId, "mine");

        bookingService.createBooking(otherUserId, otherHallId, otherSportId,
            otherDate, slotId, "theirs");

        // User should only see their own booking
        List<Booking> userBookings = bookingService.getBookingsByUserId(userId);
        assertEquals(1, userBookings.size());
        assertEquals("mine", userBookings.getFirst().getPurpose());

        // Other user should only see their booking
        List<Booking> otherBookings = bookingService.getBookingsByUserId(otherUserId);
        assertEquals(1, otherBookings.size());
        assertEquals("theirs", otherBookings.getFirst().getPurpose());
    }

    @Test
    void getBookingsByUserId_invalidUser_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.getBookingsByUserId(UUID.randomUUID())
        );
    }

    @Test
    void getBookingsByUserId_dataAccessResourceFailureException_throwsServiceUnavailable() {
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(profileRepository).findById(any(UUID.class));

        assertThrows(ServiceUnavailableException.class, () ->
            bookingService.getBookingsByUserId(userId)
        );
    }

    @Test
    void getBookingsByUserId_genericException_throwsRuntimeException() {
        doThrow(new RuntimeException("Unexpected error"))
                .when(profileRepository).findById(any(UUID.class));

        assertThrows(RuntimeException.class, () ->
            bookingService.getBookingsByUserId(userId)
        );
    }
}

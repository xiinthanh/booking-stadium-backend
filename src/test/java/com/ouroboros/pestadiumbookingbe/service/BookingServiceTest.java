package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.*;
import com.ouroboros.pestadiumbookingbe.model.*;
import com.ouroboros.pestadiumbookingbe.notifier.BookingNotificationType;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class BookingServiceTest {

    @Autowired BookingService bookingService;
    @MockitoSpyBean BookingRepository bookingRepository;
    @MockitoSpyBean ProfileRepository profileRepository;
    @MockitoSpyBean SportHallRepository sportHallRepository;
    @MockitoSpyBean SportRepository sportRepository;
    @MockitoSpyBean NotificationService notificationService;

    UUID userId, hallId, sportId;
    UUID otherUserId, otherHallId, otherSportId;
    LocalDate date, otherDate;
    LocalTime startTime, endTime, otherStartTime, otherEndTime;
    UUID adminId;

    String studentId = "12345678";
    String otherStudentId = "87654321";

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
        p.setEmail("12345678@vgu.edu.vn");
        p.setStudentId(studentId);
        p.setType(ProfileType.user);
        profileRepository.save(p);
        userId = p.getId();

        Profile otherP = new Profile();
        otherP.setEmail("87654321@vgu.edu.vn");
        otherP.setStudentId(otherStudentId);
        otherP.setType(ProfileType.user);
        profileRepository.save(otherP);
        otherUserId = otherP.getId();

        Profile admin = new Profile();
        admin.setEmail("admin@example.com");
        admin.setStudentId("admin123");
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

        // Set dates for testing
        date = LocalDate.now().plusDays(1);
        otherDate = LocalDate.now().plusDays(2);
        startTime = LocalTime.of(9, 0);
        endTime = LocalTime.of(10, 0);
        otherStartTime = LocalTime.of(10, 0);
        otherEndTime = LocalTime.of(11, 0);
    }


    @Test
    void createBooking_persistsToDb() {
        long before = bookingRepository.count();

        Booking b = bookingService.createBooking(
                userId, hallId, sportId, date, startTime, endTime
        );

        assertNotNull(b.getId());

        assertEquals(userId, b.getUserId());
        assertEquals(hallId, b.getSportHallId());
        assertEquals(date, b.getBookingDate());
        assertEquals(startTime, b.getStartTime());
        assertEquals(endTime, b.getEndTime());
        assertEquals(Status.pending, b.getStatus());

        assertEquals(before + 1, bookingRepository.count());
    }

    @Test
    void createBooking_invalidUser_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(UUID.randomUUID(), hallId, sportId,
                date, startTime, endTime
            )
        );
    }

    @Test
    void createBooking_invalidHall_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(userId, UUID.randomUUID(), sportId,
                date, startTime, endTime
            )
        );
    }

    @Test
    void createBooking_pastDate_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(userId, hallId, sportId,
                LocalDate.now().minusDays(1), startTime, endTime
            )
        );
    }

    @Test
    void createBooking_moreThanOneYearInFuture_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.createBooking(userId, hallId, sportId,
                LocalDate.now().plusYears(1).plusDays(1), startTime, endTime
            )
        );
    }

    @Test
    void createBooking_quotaExceeded_throwsForbidden() {
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);

        assertThrows(ForbiddenException.class, () ->
            bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void createBooking_adminUnlimitedQuota() {
        bookingService.createBooking(adminId, hallId, sportId, date, startTime, endTime);
        assertDoesNotThrow(() ->
                bookingService.createBooking(adminId, otherHallId, otherSportId, date, otherStartTime, otherEndTime)
        );
    }

    @Test
    void createBooking_isOccupiedPendingBooking_throwsConflict() {
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);

        // another user tries same slot
        assertThrows(ConflictException.class, () ->
            bookingService.createBooking(otherUserId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void createBooking_isOccupiedConfirmedBooking_throwsConflict() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        bookingService.confirmBooking(b.getId(), userId); // confirm the original booking

        // another user tries same slot
        assertThrows(ConflictException.class, () ->
            bookingService.createBooking(otherUserId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void createBooking_dataAccessException_throwsServiceUnavailable() {
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(profileRepository).findById(any(UUID.class));

        assertThrows(ServiceUnavailableException.class, () ->
                bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void createBooking_transactionTimeout_throwsRequestTimeout() {
        // Simulate a transaction timeout by throwing an exception
        doThrow(new TransactionTimedOutException("Transaction timed out"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RequestTimeoutException.class, () ->
                bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void createBooking_genericException_throwsRuntimeException() {
        // Simulate a generic exception
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RuntimeException.class, () ->
                bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void confirmBooking_pendingBooking() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        Booking c = bookingService.confirmBooking(b.getId(), userId);
        assertEquals(Status.confirmed, c.getStatus());
    }
    @Test
    void confirmBooking_canceledBooking() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        bookingService.cancelBooking(b.getId(), userId); // cancel first

        Booking c = bookingService.confirmBooking(b.getId(), userId);

        assertEquals(Status.confirmed, c.getStatus());
    }

    @Test
    void confirmBooking_invalidUser_throwsBadRequest() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
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
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        bookingService.confirmBooking(b.getId(), userId); // confirm first time

        assertThrows(BadRequestException.class, () ->
            bookingService.confirmBooking(b.getId(), userId) // try to confirm again
        );
    }

    @Test
    void confirmBooking_confirmRejectedBookingExceedsQuota_throwsForbidden() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        bookingService.cancelBooking(b.getId(), userId); // cancel first
        Booking c = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        // 1 spot (hallId, date, slotId) - 2 bookings (b - rejected, c - pending)

        // Now try to confirm the canceled booking which exceeds quota
        assertThrows(ForbiddenException.class, () ->
            bookingService.confirmBooking(b.getId(), userId)
        );
    }

    @Test
    void confirmBooking_dataAccessException_throwsServiceUnavailable() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(profileRepository).findById(userId);

        assertThrows(ServiceUnavailableException.class, () ->
            bookingService.confirmBooking(b.getId(), userId)
        );
    }

    @Test
    void confirmBooking_transactionTimeout_throwsRequestTimeout() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        doThrow(new TransactionTimedOutException("Transaction timed out"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RequestTimeoutException.class, () ->
            bookingService.confirmBooking(b.getId(), userId)
        );
    }

    @Test
    void confirmBooking_genericException_throwsRuntimeException() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RuntimeException.class, () ->
            bookingService.confirmBooking(b.getId(), userId)
        );
    }

    @Test
    void cancelBooking_cancelPending() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
                date, startTime, endTime);

        Booking canceled = bookingService.cancelBooking(p.getId(), userId);

        assertEquals(Status.rejected, canceled.getStatus());
        assertNotNull(canceled.getCanceledAt());
        assertEquals(canceled.getCanceledBy(), userId);
    }

    @Test
    void cancelBooking_cancelConfirmed() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
                date, startTime, endTime);
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
            date, startTime, endTime);
        assertThrows(BadRequestException.class, () ->
            bookingService.cancelBooking(p.getId(), UUID.randomUUID())
        );
    }

    @Test
    void cancelBooking_alreadyCanceled_throwsBadRequest() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        bookingService.cancelBooking(p.getId(), userId); // cancel first

        assertThrows(BadRequestException.class, () ->
            bookingService.cancelBooking(p.getId(), userId) // try to cancel again
        );
    }

    @Test
    void cancelBooking_dataAccessResourceFailureException_throwsServiceUnavailable() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(profileRepository).findById(userId);

        assertThrows(ServiceUnavailableException.class, () ->
                bookingService.cancelBooking(p.getId(), userId)
        );
    }

    @Test
    void cancelBooking_transactionTimeout_throwsRequestTimeout() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        doThrow(new TransactionTimedOutException("Transaction timed out"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RequestTimeoutException.class, () ->
            bookingService.cancelBooking(p.getId(), userId)
        );
    }

    @Test
    void cancelBooking_genericException_throwsRuntimeException() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RuntimeException.class, () ->
            bookingService.cancelBooking(p.getId(), userId)
        );
    }

    @Test
    void modifyBooking_changeDate() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
                date, startTime, endTime);

        Booking updated = bookingService.modifyBooking(
            m.getId(), userId, userId, hallId, sportId, otherDate, startTime, endTime);

        assertEquals(otherDate, updated.getBookingDate());
    }

    @Test
    void modifyBooking_changeTimeSlot() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
                date, startTime, endTime);

        Booking updated = bookingService.modifyBooking(
            m.getId(), userId, userId, hallId, sportId, date, otherStartTime, otherEndTime);

        assertEquals(otherStartTime, updated.getStartTime());
        assertEquals(otherEndTime, updated.getEndTime());
    }

    @Test
    void modifyBooking_changeSportHall() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
                date, startTime, endTime);

        Booking updated = bookingService.modifyBooking(
            m.getId(), userId, userId, otherHallId, sportId, date, startTime, endTime);

        assertEquals(otherHallId, updated.getSportHallId());
    }

    @Test
    void modifyBooking_changeAll() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
                date, startTime, endTime);

        Booking updated = bookingService.modifyBooking(
                m.getId(), otherUserId, userId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime);

        assertEquals(otherHallId, updated.getSportHallId());
        assertEquals(otherDate, updated.getBookingDate());
        assertEquals(otherStartTime, updated.getStartTime());
        assertEquals(otherEndTime, updated.getEndTime());
        assertNull(updated.getCanceledAt());
        assertNull(updated.getCanceledBy());
        assertEquals(Status.pending, updated.getStatus());
    }

    @Test
    void modifyBooking_changeConfirmedBooking() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        bookingService.confirmBooking(m.getId(), userId); // confirm first

        Booking updated = bookingService.modifyBooking(
            m.getId(), userId, userId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime);

        assertEquals(Status.pending, updated.getStatus());
    }

    @Test
    void modifyBooking_nullBookingId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(null, userId, userId, hallId, sportId,
                date, startTime, endTime)
        );
    }

    @Test
    void modifyBooking_nullModifiedByUserId_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), null, userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void modifyBooking_invalidUser_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, UUID.randomUUID(), hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void modifyBooking_invalidModifiedByUserId_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), UUID.randomUUID(), userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void modifyBooking_invalidSportHall_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, UUID.randomUUID(), sportId, date, startTime, endTime)
        );
    }

    @Test
    void modifyBooking_pastDate_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId,
                LocalDate.now().minusDays(1), startTime, endTime)
        );
    }

    @Test
    void modifyBooking_moreThanOneYearInFuture_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId,
                LocalDate.now().plusYears(1).plusDays(1), startTime, endTime)
        );
    }


    @Test
    void modifyBooking_nonexistentBooking_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(UUID.randomUUID(), userId, userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void modifyBooking_rejectedBooking_throwsBadRequest() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        bookingService.cancelBooking(m.getId(), userId); // cancel first

        assertThrows(BadRequestException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void modifyBooking_quotaExceeded_throwsForbidden() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);

        // Create another booking to fill quota
        bookingService.createBooking(userId, otherHallId, otherSportId,
            otherDate, otherStartTime, otherEndTime);

        // Now modifying should throw ForbiddenException
        assertThrows(ForbiddenException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, otherDate, startTime, endTime)
        );
    }

    @Test
    void modifyBooking_occupiedPendingBooking_throwsConflict() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        bookingService.createBooking(otherUserId, otherHallId, otherSportId,
            otherDate, otherStartTime, otherEndTime);

        // Another user tries to modify same slot
        assertThrows(ConflictException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, otherHallId, otherSportId,
                    otherDate, otherStartTime, otherEndTime)
        );
    }

    @Test
    void modifyBooking_occupiedConfirmedBooking_throwsConflict() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);

        Booking mOther = bookingService.createBooking(otherUserId, otherHallId, otherSportId,
                otherDate, otherStartTime, otherEndTime);
        bookingService.confirmBooking(mOther.getId(), otherUserId); // confirm first

        // Another user tries to modify same slot
        assertThrows(ConflictException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, otherHallId, otherSportId,
                    otherDate, otherStartTime, otherEndTime)
        );
    }

    @Test
    void modifyBooking_dataAccessResourceFailureException_throwsServiceUnavailable() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(profileRepository).findById(any(UUID.class));

        assertThrows(ServiceUnavailableException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void modifyBooking_transactionTimeout_throwsRequestTimeout() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        doThrow(new TransactionTimedOutException("Transaction timed out"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RequestTimeoutException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void modifyBooking_genericException_throwsRuntimeException() {
        Booking m = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).save(any(Booking.class));

        assertThrows(RuntimeException.class, () ->
            bookingService.modifyBooking(m.getId(), userId, userId, hallId, sportId, date, startTime, endTime)
        );
    }

    @Test
    void deleteBooking_validBooking_removesFromDb() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
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
            date, startTime, endTime);

        assertThrows(BadRequestException.class, () ->
            bookingService.deleteBooking(p.getId(), UUID.randomUUID())
        );
    }

    @Test
    void deleteBooking_dataAccessResourceFailureException_throwsServiceUnavailable() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(bookingRepository).delete(any(Booking.class));

        assertThrows(ServiceUnavailableException.class, () ->
            bookingService.deleteBooking(p.getId(), userId)
        );
    }

    @Test
    void deleteBooking_transactionTimeout_throwsRequestTimeout() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        doThrow(new TransactionTimedOutException("Transaction timed out"))
                .when(bookingRepository).delete(any(Booking.class));

        assertThrows(RequestTimeoutException.class, () ->
            bookingService.deleteBooking(p.getId(), userId)
        );
    }

    @Test
    void deleteBooking_genericException_throwsRuntimeException() {
        Booking p = bookingService.createBooking(userId, hallId, sportId,
            date, startTime, endTime);
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).delete(any(Booking.class));

        assertThrows(RuntimeException.class, () ->
            bookingService.deleteBooking(p.getId(), userId)
        );
    }

    @Test
    void createBooking_triggersNotification() {

        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);

        // Simulate afterCommit notification
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        syncs.forEach(TransactionSynchronization::afterCommit);

        verify(notificationService, times(1))
            .notifyOnBookingChange(b, BookingNotificationType.CREATION);

    }

    @Test
    void confirmBooking_triggersNotification() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        bookingService.confirmBooking(b.getId(), adminId);

        // Simulate afterCommit notification
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        syncs.forEach(TransactionSynchronization::afterCommit);

        verify(notificationService, times(1))
            .notifyOnBookingChange(b, BookingNotificationType.CONFIRMATION);
    }

    @Test
    void cancelBooking_triggersNotification() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        bookingService.cancelBooking(b.getId(), userId);

        // Simulate afterCommit notification
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        syncs.forEach(TransactionSynchronization::afterCommit);

        verify(notificationService, times(1))
            .notifyOnBookingChange(b, BookingNotificationType.CANCELLATION);
    }

    @Test
    void modifyBooking_triggersNotification() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        bookingService.modifyBooking(b.getId(), userId, userId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime);

        // Simulate afterCommit notification
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        syncs.forEach(TransactionSynchronization::afterCommit);

        verify(notificationService, times(1))
            .notifyOnBookingChange(b, BookingNotificationType.MODIFICATION);
    }

    @Test
    void deleteBooking_triggersNotification() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime);
        bookingService.deleteBooking(b.getId(), userId);

        // Simulate afterCommit notification
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        syncs.forEach(TransactionSynchronization::afterCommit);

        verify(notificationService, times(1))
            .notifyOnBookingChange(b, BookingNotificationType.DELETION);
    }
}

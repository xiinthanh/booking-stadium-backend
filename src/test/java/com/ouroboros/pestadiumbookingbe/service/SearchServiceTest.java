package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.exception.BadRequestException;
import com.ouroboros.pestadiumbookingbe.exception.ServiceUnavailableException;
import com.ouroboros.pestadiumbookingbe.model.*;
import com.ouroboros.pestadiumbookingbe.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class SearchServiceTest {

    @Autowired
    private SearchService searchService;
    @MockitoSpyBean
    private SportHallRepository sportHallRepository;
    @MockitoSpyBean
    private SportRepository sportRepository;
    @MockitoSpyBean
    private ProfileRepository profileRepository;
    @MockitoSpyBean
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

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

        // Set dates and times for testing
        date = LocalDate.now().plusDays(1);
        otherDate = LocalDate.now().plusDays(2);
        startTime = LocalTime.of(9, 0);
        endTime = LocalTime.of(10, 0);
        otherStartTime = LocalTime.of(10, 0);
        otherEndTime = LocalTime.of(11, 0);
    }

    @Test
    void getSportHallById_valid() {
        SportHall result = searchService.getSportHallById(hallId);
        assertNotNull(result);
        assertEquals(hallId, result.getId());
    }
    @Test
    void getSportHallById_invalid_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> searchService.getSportHallById(UUID.randomUUID()));
    }
    @Test
    void getSportHallById_dataAccessResourceFailure_throwsServiceUnavailable() {
        doThrow(DataAccessResourceFailureException.class)
                .when(sportHallRepository).findById(hallId);

        assertThrows(ServiceUnavailableException.class, () -> searchService.getSportHallById(hallId));
    }
    @Test
    void getSportHallById_genericException_throwsRuntimeException() {
        doThrow(RuntimeException.class)
                .when(sportHallRepository).findById(hallId);

        assertThrows(RuntimeException.class, () -> searchService.getSportHallById(hallId));
    }


    @Test
    void getAllBookings_returnsAllBookings() {
        bookingRepository.deleteAll();
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "first");
        bookingService.createBooking(otherUserId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime, "second");
        List<Booking> bookings = searchService.getAllBookings();
        assertEquals(2, bookings.size());
    }

    @Test
    void getAllBookings_dataAccessResourceFailureException_throwsServiceUnavailable() {
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(bookingRepository).findAll();

        assertThrows(ServiceUnavailableException.class, () ->
                searchService.getAllBookings()
        );
    }

    @Test
    void getAllBookings_genericException_throwsRuntimeException() {
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).findAll();

        assertThrows(RuntimeException.class, () ->
                searchService.getAllBookings()
        );
    }

    @Test
    void getBookingById_valid() {
        Booking b = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "test");
        Booking found = searchService.getBookingById(b.getId());
        assertEquals(b.getId(), found.getId());
    }
    @Test
    void getBookingById_invalid() {
        assertThrows(BadRequestException.class, () -> searchService.getBookingById(UUID.randomUUID()));
    }
    @Test
    void getBookingById_dataAccessResourceFailureException_throwsServiceUnavailable() {
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(bookingRepository).findById(any(UUID.class));

        assertThrows(ServiceUnavailableException.class, () ->
                searchService.getBookingById(UUID.randomUUID())
        );
    }

    @Test
    void getBookingById_genericException_throwsRuntimeException() {
        doThrow(new RuntimeException("Unexpected error"))
                .when(bookingRepository).findById(any(UUID.class));

        assertThrows(RuntimeException.class, () ->
                searchService.getBookingById(UUID.randomUUID())
        );
    }

    @Test
    void getBookingsByUserId_existingUser_returnsUserBookings() {
        // Clear any existing bookings
        bookingRepository.deleteAll();

        // Create booking for our test user
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "mine");

        bookingService.createBooking(otherUserId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime, "theirs");

        // User should only see their own booking
        List<Booking> userBookings = searchService.getBookingsByUserId(userId);
        assertEquals(1, userBookings.size());
        assertEquals("mine", userBookings.get(0).getPurpose());

        // Other user should only see their booking
        List<Booking> otherBookings = searchService.getBookingsByUserId(otherUserId);
        assertEquals(1, otherBookings.size());
        assertEquals("theirs", otherBookings.get(0).getPurpose());
    }

    @Test
    void getBookingsByUserId_invalidUser_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                searchService.getBookingsByUserId(UUID.randomUUID())
        );
    }

    @Test
    void getBookingsByUserId_dataAccessResourceFailureException_throwsServiceUnavailable() {
        doThrow(new DataAccessResourceFailureException("Database error"))
                .when(profileRepository).findById(any(UUID.class));

        assertThrows(ServiceUnavailableException.class, () ->
                searchService.getBookingsByUserId(userId)
        );
    }

    @Test
    void getBookingsByUserId_genericException_throwsRuntimeException() {
        doThrow(new RuntimeException("Unexpected error"))
                .when(profileRepository).findById(any(UUID.class));

        assertThrows(RuntimeException.class, () ->
                searchService.getBookingsByUserId(userId)
        );
    }

    @Test
    void filterBookings_noFilters_returnsAllBookings() {
        Booking b1 = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "p1");
        Booking b2 = bookingService.createBooking(otherUserId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime, "p2");
        List<Booking> result = searchService.filterBookings(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(2, result.size());
        assertTrue(result.stream().map(Booking::getId).toList().containsAll(List.of(b1.getId(), b2.getId())));
    }

    @Test
    void filterBookings_userWithEmptyStudentId_skipsUser() {
        // Create a user with an empty student ID
        Profile newUser = new Profile();
        newUser.setEmail("1234567@student.vgu.edu.vn");
        newUser.setType(ProfileType.user);
        profileRepository.save(newUser);

        // request filter should not include this user
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "p1");
        bookingService.createBooking(newUser.getId(), otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime, "p2");
        List<Booking> result = searchService.filterBookings(Optional.of(studentId.substring(0, 3)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(1, result.size());
    }

    @Test
    void filterBookings_noMatchedStudentId_returnsEmptyList() {
        // Create bookings with different student IDs
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "p1");
        bookingService.createBooking(otherUserId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime, "p2");
        List<Booking> result = searchService.filterBookings(Optional.of("nonexistent"), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(0, result.size());
    }

    @Test
    void filterBookings_emptyStringStudentId_returnsAllBookings() {
        Booking b1 = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "p1");
        Booking b2 = bookingService.createBooking(otherUserId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime, "p2");
        List<Booking> result = searchService.filterBookings(Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(2, result.size());
        assertTrue(result.stream().map(Booking::getId).toList().containsAll(List.of(b1.getId(), b2.getId())));
    }

    @Test
    void filterBookings_partialStudentId_returnsFilteredBookings() {
        Booking b1 = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "p1");
        Booking b2 = bookingService.createBooking(userId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime, "p2");
        bookingService.createBooking(otherUserId, otherHallId, otherSportId, date, otherStartTime, otherEndTime, "p3");
        List<Booking> result = searchService.filterBookings(Optional.of(studentId.substring(0, 5)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(2, result.size());
        assertTrue(result.stream().map(Booking::getId).toList().containsAll(List.of(b1.getId(), b2.getId())));
    }

    @Test
    void filterBookings_byUserId() {
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "p1");
        bookingService.createBooking(userId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime, "p2");
        bookingService.createBooking(otherUserId, otherHallId, otherSportId, date, otherStartTime, otherEndTime, "p3");
        List<Booking> result = searchService.filterBookings(Optional.of(studentId), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(2, result.size());
        assertEquals(userId, result.get(0).getUserId());
    }

    @Test
    void filterBookings_byStatus() {
        bookingRepository.deleteAll();
        Booking b1 = bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "one");
        Booking b2 = bookingService.createBooking(userId, hallId, sportId, otherDate, otherStartTime, otherEndTime, "two");
        bookingService.confirmBooking(b2.getId(), adminId);
        List<Booking> pending = searchService.filterBookings(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Status.pending));
        assertTrue(pending.stream().allMatch(b -> b.getStatus()==Status.pending));
    }

    @Test
    void filterBookings_byLocation() {
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "p1");
        bookingService.createBooking(otherUserId, otherHallId, sportId, date, startTime, endTime, "p2");
        List<Booking> indoor = searchService.filterBookings(Optional.empty(), Optional.of(SportHallLocation.indoor), Optional.empty(), Optional.empty());
        List<Booking> outdoor = searchService.filterBookings(Optional.empty(), Optional.of(SportHallLocation.outdoor), Optional.empty(), Optional.empty());
        assertEquals(1, indoor.size());
        assertEquals(hallId, indoor.get(0).getSportHallId());
        assertEquals(1, outdoor.size());
        assertEquals(otherHallId, outdoor.get(0).getSportHallId());
    }

    @Test
    void filterBookings_byProfileType() {
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "p1");
        bookingService.createBooking(adminId, otherHallId, sportId, date, startTime, endTime, "p2");
        List<Booking> users = searchService.filterBookings(Optional.empty(), Optional.empty(), Optional.of(ProfileType.user), Optional.empty());
        List<Booking> admins = searchService.filterBookings(Optional.empty(), Optional.empty(), Optional.of(ProfileType.admin), Optional.empty());
        assertEquals(1, users.size());
        assertEquals(userId, users.get(0).getUserId());
        assertEquals(1, admins.size());
        assertEquals(adminId, admins.get(0).getUserId());
    }

    @Test
    void filterBookings_noMatchedLocation_returnsEmptyList() {
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "p1");
        bookingService.createBooking(otherUserId, hallId, sportId, otherDate, otherStartTime, otherEndTime, "p2");
        List<Booking> result = searchService.filterBookings(Optional.empty(), Optional.of(SportHallLocation.outdoor), Optional.empty(), Optional.empty());
        assertEquals(0, result.size());
    }

    @Test
    void filterBookings_combinedFilters() {
        bookingService.createBooking(userId, hallId, sportId, date, startTime, endTime, "p1");
        Booking b2 = bookingService.createBooking(userId, otherHallId, otherSportId, otherDate, otherStartTime, otherEndTime, "p2");
        bookingService.confirmBooking(b2.getId(), adminId);
        List<Booking> result = searchService.filterBookings(
                Optional.of(studentId),
                Optional.of(SportHallLocation.outdoor),
                Optional.of(ProfileType.user),
                Optional.of(Status.confirmed)
        );
        assertEquals(1, result.size());
        assertEquals(b2.getId(), result.get(0).getId());
    }
}

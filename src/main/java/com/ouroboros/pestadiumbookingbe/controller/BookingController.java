package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.BookingRequest;
import com.ouroboros.pestadiumbookingbe.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping("/create-booking")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest bookingRequest) {
        // Delegate to the service layer
        return bookingService.createBooking(
            bookingRequest.getUserId(),
            bookingRequest.getSportHallId(),
            bookingRequest.getDate(),
            bookingRequest.getTimeSlotId(),
            bookingRequest.getPurpose()
        );
    }

    @GetMapping("/get-bookings")
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }
}

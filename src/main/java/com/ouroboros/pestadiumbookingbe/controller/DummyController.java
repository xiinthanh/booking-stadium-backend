package com.ouroboros.pestadiumbookingbe.controller;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.Sport;
import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dummy")
public class DummyController {
    @GetMapping("/dummy-booking")
    public Booking dummyBooking() {
        return new Booking();
    }

    @GetMapping("/dummy-sport")
    public Sport dummySport() {
        return new Sport();
    }

    @GetMapping("/dummy-sport-hall")
    public SportHall dummySportHall() {
        return new SportHall();
    }

    @GetMapping("/dummy-time-slot")
    public TimeSlot dummyTimeSlot() {
        return new TimeSlot();
    }
}

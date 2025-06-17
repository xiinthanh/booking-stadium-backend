package com.ouroboros.pestadiumbookingbe.util;

import com.ouroboros.pestadiumbookingbe.dto.BookingSummary;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.service.TimeSlotService;
import com.ouroboros.pestadiumbookingbe.service.SportHallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {
    private final TimeSlotService timeSlotService;
    private final SportHallService sportHallService;

    @Autowired
    public BookingMapper(TimeSlotService timeSlotService, SportHallService sportHallService) {
        this.timeSlotService = timeSlotService;
        this.sportHallService = sportHallService;
    }

    public BookingSummary toBookingSummary(Booking booking) {
        TimeSlot timeSlot = booking.getTimeSlotId() != null ? timeSlotService.getTimeSlotById(booking.getTimeSlotId()) : null;
        SportHall sportHall = booking.getSportHallId() != null ? sportHallService.getSportHallById(booking.getSportHallId()) : null;
        return new BookingSummary(
                booking.getBookingDate(),
                timeSlot != null ? timeSlot.getStartTime() : null,
                timeSlot != null ? timeSlot.getEndTime() : null,
                sportHall != null ? sportHall.getName() : null,
                booking.getPurpose()
        );
    }
}


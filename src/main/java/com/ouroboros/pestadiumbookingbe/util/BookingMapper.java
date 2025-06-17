package com.ouroboros.pestadiumbookingbe.util;

import com.ouroboros.pestadiumbookingbe.dto.BookingSummary;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.service.ProfileService;
import com.ouroboros.pestadiumbookingbe.service.TimeSlotService;
import com.ouroboros.pestadiumbookingbe.service.SportHallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {
    private final TimeSlotService timeSlotService;
    private final SportHallService sportHallService;
    private final ProfileService profileService;

    @Autowired
    public BookingMapper(TimeSlotService timeSlotService, SportHallService sportHallService, ProfileService profileService) {
        this.timeSlotService = timeSlotService;
        this.sportHallService = sportHallService;
        this.profileService = profileService;
    }

    public BookingSummary toBookingSummary(Booking booking) {
        TimeSlot timeSlot = booking.getTimeSlotId() != null ? timeSlotService.getTimeSlotById(booking.getTimeSlotId()) : null;
        SportHall sportHall = booking.getSportHallId() != null ? sportHallService.getSportHallById(booking.getSportHallId()) : null;
        String emailAddress = booking.getUserId() != null ? profileService.getEmailByUserId(booking.getUserId()) : null;
        return new BookingSummary(
                emailAddress != null ? emailAddress : "",
                booking.getBookingDate(),
                timeSlot != null ? timeSlot.getStartTime() : null,
                timeSlot != null ? timeSlot.getEndTime() : null,
                sportHall != null ? sportHall.getName() : null,
                booking.getPurpose()
        );
    }
}


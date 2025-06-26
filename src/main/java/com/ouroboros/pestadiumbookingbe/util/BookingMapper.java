package com.ouroboros.pestadiumbookingbe.util;

import com.ouroboros.pestadiumbookingbe.dto.BookingSummary;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.model.SportHall;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import com.ouroboros.pestadiumbookingbe.repository.SportHallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private SportHallRepository sportHallRepository;

    private static final Logger logger = LoggerFactory.getLogger(BookingMapper.class);

    public BookingSummary toBookingSummary(Booking booking) {
        if (booking == null) {
            logger.warn("Booking is null, returning empty BookingSummary");
            return new BookingSummary("", null, null, null, "", "", "");
        }

        SportHall sportHall = null;
        Profile userProfile = null;
        Profile canceledByProfile = null;
        try {
            sportHall = booking.getSportHallId() != null ? sportHallRepository.findById(booking.getSportHallId()).orElse(null) : null;
            userProfile = booking.getUserId() != null ? profileRepository.findById(booking.getUserId()).orElse(null) : null;
            canceledByProfile = booking.getCanceledBy() != null ? profileRepository.findById(booking.getCanceledBy()).orElse(null) : null;
        } catch (Exception e) {
            logger.error("Error fetching related entities for booking ID: {} from BookingMapper class", booking.getId(), e);
        }
        return new BookingSummary(
                userProfile != null ? userProfile.getEmail() : "",
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                sportHall != null ? sportHall.getName() : null,
                booking.getPurpose(),
                canceledByProfile != null ? canceledByProfile.getEmail() : ""
        );
    }
}

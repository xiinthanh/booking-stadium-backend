package com.ouroboros.pestadiumbookingbe.notifier;

import com.ouroboros.pestadiumbookingbe.dto.BookingSummary;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.util.BookingMapper;
import com.ouroboros.pestadiumbookingbe.util.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BookingCreationHandler implements BookingNotificationHandler {
    @Autowired
    private BookingMapper bookingMapper;
    @Autowired
    private EmailSender emailSender;

    private static final Logger logger = LoggerFactory.getLogger(BookingCreationHandler.class);

    @Override
    public void notify(Booking booking) {
        logger.info("Sending booking creation email for booking: {}", booking.getId());

        try {
            BookingSummary bookingSummary = bookingMapper.toBookingSummary(booking);

            String subject = "Booking Creation";
            String text = String.format("Your booking for %s on %s from %s to %s has been created",
                    bookingSummary.getSportHallName(),
                    bookingSummary.getBookingDate(),
                    bookingSummary.getStartTime(),
                    bookingSummary.getEndTime());
            emailSender.sendEmail(bookingSummary.getSenderEmailAddress(), subject, text);
        } catch (Exception e) {
            logger.error("Failed to send booking rejection email for booking: {}", booking.getId(), e);
        }
    }

    @Override
    public BookingNotificationType getType() {
        return BookingNotificationType.CREATION;
    }
}
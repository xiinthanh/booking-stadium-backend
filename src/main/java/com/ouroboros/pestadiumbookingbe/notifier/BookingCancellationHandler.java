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
public class BookingCancellationHandler implements BookingNotificationHandler {
    @Autowired
    private BookingMapper bookingMapper;
    @Autowired
    private EmailSender emailSender;

    private static final Logger logger = LoggerFactory.getLogger(BookingCancellationHandler.class);

    @Override
    public void notify(Booking booking) {
        logger.info("Sending booking cancellation email for booking: {}", booking.getId());

        BookingSummary bookingSummary = bookingMapper.toBookingSummary(booking);
        try {
            String subject = "Booking Cancellation";
            String text = String.format("Your booking for %s on %s from %s to %s has been cancelled by %s.",
                    bookingSummary.getSportHallName(),
                    bookingSummary.getBookingDate(),
                    bookingSummary.getStartTime(),
                    bookingSummary.getEndTime(),
                    bookingSummary.getCanceledByEmailAddress());
            emailSender.sendEmail(bookingSummary.getSenderEmailAddress(), subject, text);
        } catch (Exception e) {
            logger.error("Failed to send booking cancellation email for booking: {}", booking.getId(), e);
        }
    }

    @Override
    public BookingNotificationType getType() {
        return BookingNotificationType.CANCELLATION;
    }
}
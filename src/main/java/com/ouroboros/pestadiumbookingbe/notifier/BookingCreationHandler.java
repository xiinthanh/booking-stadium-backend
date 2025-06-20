package com.ouroboros.pestadiumbookingbe.notifier;

import com.ouroboros.pestadiumbookingbe.dto.BookingSummary;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.util.BookingMapper;
import com.ouroboros.pestadiumbookingbe.util.EmailSender;
import com.ouroboros.pestadiumbookingbe.util.IcsFileGenerator;
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
    @Autowired
    private IcsFileGenerator icsFileGenerator;

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
            byte[] icsBytes = icsFileGenerator.generateIcsStream(bookingSummary).toByteArray();
            emailSender.sendEmailWithIcsAttachment(bookingSummary.getSenderEmailAddress(), subject, text, icsBytes);
        } catch (Exception e) {
            logger.error("Failed to send booking creation email for booking: {}", booking.getId(), e);
        }
    }

    @Override
    public BookingNotificationType getType() {
        return BookingNotificationType.CREATION;
    }
}
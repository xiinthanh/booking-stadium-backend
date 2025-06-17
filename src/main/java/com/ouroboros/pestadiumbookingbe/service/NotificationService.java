package com.ouroboros.pestadiumbookingbe.service;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import com.ouroboros.pestadiumbookingbe.dto.BookingSummary;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.util.BookingMapper;
import com.ouroboros.pestadiumbookingbe.util.IcsFileGenerator;
import feign.form.FormData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    @Value("${mailgun.api.key}")
    private String apiKey;

    @Value("${mailgun.domain}")
    private String domain;

    @Value("${mailgun.from.email}")
    private String emailFrom;

    @Autowired
    private BookingMapper bookingMapper;

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void sendEmail(String to, String subject, String text) {
        try {
            MailgunMessagesApi mailgunMessagesApi = MailgunClient.config(apiKey)
                    .createApi(MailgunMessagesApi.class);
            Message message = Message.builder()
                    .from(emailFrom)
                    .to(to)
                    .subject(subject)
                    .text(text)
                    .build();

            // Send the email asynchronously
            CompletableFuture<MessageResponse> response = mailgunMessagesApi.sendMessageAsync(domain, message);

            logger.info("Email ({}) sent successfully to {}", text, to);
        } catch (Exception e) {
            logger.error("Failed to send email ({}) to {}: {}", text, to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendEmailWithIcsAttachment(String to, String subject, String text, byte[] icsBytes) {
        logger.info("Sending email with Ics attachment to {}", to);
        try {
            MailgunMessagesApi mailgunMessagesApi = MailgunClient.config(apiKey)
                    .createApi(MailgunMessagesApi.class);

            // Write the ICS content to a temporary file
            File tempFile = File.createTempFile("booking", ".ics");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(icsBytes);
            }

            Message message = Message.builder()
                    .from(emailFrom)
                    .to(to)
                    .subject(subject)
                    .text(text)
                    .attachment(tempFile)
                    .build();

            CompletableFuture<MessageResponse> response = mailgunMessagesApi.sendMessageAsync(domain, message);

            logger.info("Email with .ics attachment sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email with attachment", e);
        }
    }

    public enum BookingNotificationType {
        CONFIRMATION, CANCELLATION, REJECTION
    }

    public void notifyOnBookingChange(Booking booking, BookingNotificationType type) {
        BookingSummary bookingSummary = bookingMapper.toBookingSummary(booking);
        String subject;
        String text;
        switch (type) {
            case CONFIRMATION:
                subject = "Booking Confirmation";
                text = String.format("Your booking for %s on %s from %s to %s has been confirmed.",
                        bookingSummary.getSportHallName(),
                        bookingSummary.getBookingDate(),
                        bookingSummary.getStartTime(),
                        bookingSummary.getEndTime());
                break;
            case CANCELLATION:
                subject = "Booking Cancellation";
                text = String.format("Your booking for %s on %s from %s to %s has been cancelled.",
                        bookingSummary.getSportHallName(),
                        bookingSummary.getBookingDate(),
                        bookingSummary.getStartTime(),
                        bookingSummary.getEndTime());
                break;
            case REJECTION:
                subject = "Booking Rejection";
                text = String.format("Your booking for %s on %s from %s to %s has been rejected.",
                        bookingSummary.getSportHallName(),
                        bookingSummary.getBookingDate(),
                        bookingSummary.getStartTime(),
                        bookingSummary.getEndTime());
                break;
            default:
                throw new IllegalArgumentException("Unknown booking change type");
        }
        sendEmail(booking.getUserId().toString(), subject, text);
    }
}

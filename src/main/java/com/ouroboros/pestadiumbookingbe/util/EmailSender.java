package com.ouroboros.pestadiumbookingbe.util;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class EmailSender {

    @Value("${mailgun.api.key}")
    private String apiKey;

    @Value("${mailgun.domain}")
    private String domain;

    @Value("${mailgun.from.email}")
    private String emailFrom;

    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    public void sendEmail(String to, String subject, String text) {
        logger.info("Sending email to {}", to);
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
            response.exceptionally(e -> {
                logger.error("Retrying email due to failure: {}", e.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(2); // Retry after 2 seconds
                    return mailgunMessagesApi.sendMessage(domain, message);
                } catch (Exception retryException) {
                    logger.error("Retry failed: {}", retryException.getMessage());
                    throw new RuntimeException("Failed to send email after retry", retryException);
                }
            });

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
            response.exceptionally(e -> {
                logger.error("Retrying email with attachment due to failure: {}", e.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(2); // Retry after 2 seconds
                    return mailgunMessagesApi.sendMessage(domain, message);
                } catch (Exception retryException) {
                    logger.error("Retry failed: {}", retryException.getMessage());
                    throw new RuntimeException("Failed to send email with attachment after retry", retryException);
                }
            });

            logger.info("Email with .ics attachment sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email with attachment", e);
        }
    }
}

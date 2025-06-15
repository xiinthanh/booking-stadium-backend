package com.ouroboros.pestadiumbookingbe.service;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class MyMailgunService {

    @Value("${mailgun.api.key}")
    private String apiKey;

    @Value("${mailgun.domain}")
    private String domain;

    @Value("${mailgun.from.email}")
    private String emailFrom;

    private static final Logger logger = LoggerFactory.getLogger(MyMailgunService.class);

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

}

package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.notifier.BookingNotificationHandler;
import com.ouroboros.pestadiumbookingbe.notifier.BookingNotificationType;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private List<BookingNotificationHandler> handlers;

    @Test
    void handlerCountMatchesNotificationTypes() {
        assertEquals(BookingNotificationType.values().length, handlers.size());
    }

    @Test
    void notifyOnBookingChange_doesNotThrow() {
        Booking dummy = new Booking();
        for (BookingNotificationType type : BookingNotificationType.values()) {
            assertDoesNotThrow(() -> notificationService.notifyOnBookingChange(dummy, type));
        }
    }

    @Test
    void notifyOnBookingChange_withInvalidType_doesNotThrow() {
        Booking dummy = new Booking();
        assertDoesNotThrow(() -> notificationService.notifyOnBookingChange(dummy, null));
    }
}

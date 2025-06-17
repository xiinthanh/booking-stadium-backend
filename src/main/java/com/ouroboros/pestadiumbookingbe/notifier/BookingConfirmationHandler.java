package com.ouroboros.pestadiumbookingbe.notifier;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingConfirmationHandler implements BookingNotificationHandler {
    // Inject dependencies as needed
    @Override
    public void notify(Booking booking) {
        // TODO:
    }
}
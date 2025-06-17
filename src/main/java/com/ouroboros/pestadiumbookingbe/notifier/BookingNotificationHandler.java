package com.ouroboros.pestadiumbookingbe.notifier;

import com.ouroboros.pestadiumbookingbe.model.Booking;

public interface BookingNotificationHandler {
    void notify(Booking booking);
    BookingNotificationType getType();
}
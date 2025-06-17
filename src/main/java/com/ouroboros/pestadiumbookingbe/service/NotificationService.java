package com.ouroboros.pestadiumbookingbe.service;

import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.notifier.BookingNotificationHandler;
import com.ouroboros.pestadiumbookingbe.notifier.BookingNotificationType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final Map<BookingNotificationType, BookingNotificationHandler> handlerMap = new EnumMap<>(BookingNotificationType.class);

    @Autowired
    public NotificationService(List<BookingNotificationHandler> handlers) {
        for (BookingNotificationHandler handler : handlers) {
            handlerMap.put(handler.getType(), handler);
        }
    }

    public void notifyOnBookingChange(Booking booking, BookingNotificationType type) {
        BookingNotificationHandler handler = handlerMap.get(type);
        if (handler != null) {
            handler.notify(booking);
        } else {
            logger.warn("No handler found for notification type: {}", type);
        }
    }
}

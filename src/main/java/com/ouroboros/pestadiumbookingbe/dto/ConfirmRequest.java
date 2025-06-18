package com.ouroboros.pestadiumbookingbe.dto;

import java.util.UUID;

public class ConfirmRequest {

    private UUID bookingId;
    private UUID confirmedBy;

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(UUID confirmedBy) {
        this.confirmedBy = confirmedBy;
    }
}

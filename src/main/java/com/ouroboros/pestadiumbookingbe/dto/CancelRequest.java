package com.ouroboros.pestadiumbookingbe.dto;

import java.util.UUID;

public class CancelRequest {

    private UUID bookingId;
    private UUID canceledBy;

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public UUID getCanceledBy() {
        return canceledBy;
    }

    public void setCanceledBy(UUID canceledBy) {
        this.canceledBy = canceledBy;
    }
}

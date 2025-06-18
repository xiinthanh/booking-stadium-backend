package com.ouroboros.pestadiumbookingbe.dto;

import java.util.UUID;
import java.time.LocalDate;

public class ModifyRequest {

    private UUID bookingId;
    private UUID modifiedByUserId;
    private UUID userId;
    private UUID sportHallId;
    private UUID sportId;
    private LocalDate date;
    private UUID timeSlotId;
    private String purpose;

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public UUID getModifiedByUserId() {
        return modifiedByUserId;
    }

    public void setModifiedByUserId(UUID modifiedByUserId) {
        this.modifiedByUserId = modifiedByUserId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getSportHallId() {
        return sportHallId;
    }

    public void setSportHallId(UUID sportHallId) {
        this.sportHallId = sportHallId;
    }

    public UUID getSportId() {
        return sportId;
    }

    public void setSportId(UUID sportId) {
        this.sportId = sportId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public UUID getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(UUID timeSlotId) {
        this.timeSlotId = timeSlotId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}

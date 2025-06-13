package com.ouroboros.pestadiumbookingbe.model;

import java.time.LocalDate;
import java.util.UUID;

public class BookingRequest {
    private UUID userId;
    private UUID sportHallId;
    private LocalDate date;
    private UUID timeSlotId;
    private String purpose;

    // Getters and setters
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

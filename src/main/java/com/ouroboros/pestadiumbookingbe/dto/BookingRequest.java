package com.ouroboros.pestadiumbookingbe.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class BookingRequest {
    private UUID userId;
    private UUID sportHallId;
    private UUID sportId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public UUID getSportId() {
        return sportId;
    }

    public void setSportId(UUID sportId) {
        this.sportId = sportId;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}

package com.ouroboros.pestadiumbookingbe.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class BookingSummary {
    private String senderEmailAddress;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String sportHallName;
    private String purpose;
    private String canceledByEmailAddress;

    public BookingSummary() {}

    public BookingSummary(String senderEmailAddress, LocalDate bookingDate, LocalTime startTime, LocalTime endTime, String sportHallName, String purpose, String cancledByEmailAddress) {
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sportHallName = sportHallName;
        this.purpose = purpose;
        this.senderEmailAddress = senderEmailAddress;
        this.canceledByEmailAddress = cancledByEmailAddress;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
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

    public String getSportHallName() {
        return sportHallName;
    }

    public void setSportHallName(String sportHallName) {
        this.sportHallName = sportHallName;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getSenderEmailAddress() {
        return senderEmailAddress;
    }

    public void setSenderEmailAddress(String senderEmailAddress) {
        this.senderEmailAddress = senderEmailAddress;
    }

    public String getCanceledByEmailAddress() {
        return canceledByEmailAddress;
    }

    public void setCanceledByEmailAddress(String canceledByEmailAddress) {
        this.canceledByEmailAddress = canceledByEmailAddress;
    }
}


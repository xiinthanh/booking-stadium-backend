package com.ouroboros.pestadiumbookingbe.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sport_hall_id", "booking_date", "time_slot_id"})
})
public class Booking {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "sport_hall_id", nullable = false)
    private UUID sportHallId;

    @Column(name = "sportId", nullable = false)
    private UUID sportId;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "time_slot_id", nullable = false)
    private UUID timeSlotId;

    @Column(nullable = false)
    private Integer participants = 1;

    @Column(columnDefinition = "text")
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "canceled_at")
    private OffsetDateTime canceledAt;

    @Column(name = "canceled_by")
    private UUID canceledBy;

    @Column(name = "totalCost", precision = 10, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "specialRequirements", columnDefinition = "text")
    private String specialRequirements;

    @Column(name = "created_at", columnDefinition = "timestamp with time zone")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", columnDefinition = "timestamp with time zone")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSportHallId() { return sportHallId; }
    public void setSportHallId(UUID sportHallId) { this.sportHallId = sportHallId; }

    public UUID getSportId() { return sportId; }
    public void setSportId(UUID sportId) { this.sportId = sportId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public UUID getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(UUID timeSlotId) { this.timeSlotId = timeSlotId; }

    public Integer getParticipants() { return participants; }
    public void setParticipants(Integer participants) { this.participants = participants; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public OffsetDateTime getCanceledAt() { return canceledAt; }
    public void setCanceledAt(OffsetDateTime canceledAt) { this.canceledAt = canceledAt; }

    public UUID getCanceledBy() { return canceledBy; }
    public void setCanceledBy(UUID canceledBy) { this.canceledBy = canceledBy; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

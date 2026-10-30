package com.bordeauxbrothers.parkshark.allocation.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "parking_spot_allocation", schema = "parkshark")
public class ParkingSpotAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Positive
    @Column(name = "member_id", nullable = false, updatable = false)
    private Integer memberId;

    @NotNull
    @Positive
    @Column(name = "parking_lot_id", nullable = false, updatable = false)
    private Integer parkingLotId;

    @NotBlank
    @Size(max = 30)
    @Column(
            name = "license_plate_number",
            nullable = false,
            updatable = false,
            length = 30
    )
    private String licensePlateNumber;

    @NotNull
    @Column(name = "start_time", nullable = false, updatable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AllocationStatus status;

    protected ParkingSpotAllocation() {

    }

    public ParkingSpotAllocation(
            Integer memberId,
            Integer parkingLotId,
            String licensePlateNumber,
            OffsetDateTime startTime) {

        this.memberId = memberId;
        this.parkingLotId = parkingLotId;
        this.licensePlateNumber = licensePlateNumber;
        this.startTime = startTime;
        this.status = AllocationStatus.ACTIVE;
    }

    public void stop(OffsetDateTime endTime) {
        if (status != AllocationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only an active allocation can be stopped"
            );
        }

        Objects.requireNonNull(endTime, "End time is required");

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                    "End time cannot be before start time"
            );
        }

        this.endTime = endTime;
        this.status = AllocationStatus.PASSIVE;
    }

    public void markAsInvoiced() {
        if (status != AllocationStatus.PASSIVE) {
            throw new IllegalStateException(
                    "Only a stopped allocation can be invoiced"
            );
        }

        this.status = AllocationStatus.INVOICED;
    }

    public Integer getId() {
        return id;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public Integer getParkingLotId() {
        return parkingLotId;
    }

    public String getLicensePlateNumber() {
        return licensePlateNumber;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public AllocationStatus getStatus() {
        return status;
    }
}
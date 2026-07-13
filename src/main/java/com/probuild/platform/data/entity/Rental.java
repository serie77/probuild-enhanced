package com.probuild.platform.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A tool-hire booking. */
@Entity
@Table(name = "RENTALS")
public class Rental {
    @Id @Column(name = "BOOKING_REFERENCE") public String bookingReference;
    @Column(name = "JOURNEY_ID") public String journeyId;
    @Column(name = "TOOL_CODE") public String toolCode;
    @Column(name = "RENTAL_DAYS") public double rentalDays;
    @Column(name = "DAILY_RATE") public double dailyRate;
    @Column(name = "RENTAL_FEE") public double rentalFee;
    @Column(name = "DEPOSIT_AMOUNT") public double depositAmount;
    @Column(name = "TIER_LABEL") public String tierLabel;
    @Column(name = "STATUS") public String status;
    @Column(name = "ISSUED_AT") public LocalDateTime issuedAt;
    @Column(name = "UPDATED_AT") public LocalDateTime updatedAt;
}

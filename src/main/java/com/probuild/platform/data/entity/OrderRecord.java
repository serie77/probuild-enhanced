package com.probuild.platform.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A retail order raised by the Warehouse capability. */
@Entity
@Table(name = "ORDERS")
public class OrderRecord {
    @Id @Column(name = "ORDER_REFERENCE") public String orderReference;
    @Column(name = "JOURNEY_ID") public String journeyId;
    @Column(name = "CUSTOMER_NAME") public String customerName;
    @Column(name = "PRODUCT_CODE") public String productCode;
    @Column(name = "QUANTITY") public int quantity;
    @Column(name = "QUOTED_PRICE") public double quotedPrice;
    @Column(name = "FINAL_AMOUNT") public double finalAmount;
    @Column(name = "TRACKING_NUMBER") public String trackingNumber;
    @Column(name = "STATUS") public String status;
    @Column(name = "CREATED_AT") public LocalDateTime createdAt;
    @Column(name = "UPDATED_AT") public LocalDateTime updatedAt;
}

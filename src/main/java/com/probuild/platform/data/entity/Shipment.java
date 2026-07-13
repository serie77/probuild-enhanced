package com.probuild.platform.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** An outbound or inbound goods movement. */
@Entity
@Table(name = "SHIPMENTS")
public class Shipment {
    @Id @Column(name = "TRACKING_NUMBER") public String trackingNumber;
    @Column(name = "ORDER_REFERENCE") public String orderReference;
    @Column(name = "JOURNEY_ID") public String journeyId;
    @Column(name = "DIRECTION") public String direction;   // OUTBOUND | INBOUND
    @Column(name = "DESTINATION") public String destination;
    @Column(name = "SENT_AT") public LocalDateTime sentAt;
}

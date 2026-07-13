package com.probuild.platform.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A FixPro maintenance invoice. */
@Entity
@Table(name = "SERVICE_INVOICES")
public class ServiceInvoice {
    @Id @Column(name = "INVOICE_REFERENCE") public String invoiceReference;
    @Column(name = "SERVICE_REFERENCE") public String serviceReference;
    @Column(name = "JOURNEY_ID") public String journeyId;
    @Column(name = "LABOUR_COST") public double labourCost;
    @Column(name = "PARTS_COST") public double partsCost;
    @Column(name = "CALL_OUT_COST") public double callOutCost;
    @Column(name = "TOTAL_AMOUNT") public double totalAmount;
    @Column(name = "ISSUED_AT") public LocalDateTime issuedAt;
}

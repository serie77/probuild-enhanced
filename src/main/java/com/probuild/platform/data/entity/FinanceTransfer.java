package com.probuild.platform.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A FinTrust finance decision record. */
@Entity
@Table(name = "FINANCE_TRANSFERS")
public class FinanceTransfer {
    @Id @Column(name = "TRANSFER_REFERENCE") public String transferReference;
    @Column(name = "JOURNEY_ID") public String journeyId;
    @Column(name = "CUSTOMER_NAME") public String customerName;
    @Column(name = "AMOUNT") public double amount;
    @Column(name = "TERM_MONTHS") public int termMonths;
    @Column(name = "STATUS") public String status;            // APPROVED | REJECTED
    @Column(name = "RECOMMENDATION") public String recommendation; // DMN output
    @Column(name = "CREDIT_SCORE") public Integer creditScore;
    @Column(name = "DECIDED_AT") public LocalDateTime decidedAt;
}

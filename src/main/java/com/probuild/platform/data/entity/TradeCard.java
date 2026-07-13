package com.probuild.platform.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A ProBuild Trade Card: loyalty membership with a points balance and discount band. */
@Entity
@Table(name = "TRADE_CARDS")
public class TradeCard {

    @Id
    @Column(name = "TRADE_CARD_NUMBER")
    private String tradeCardNumber;

    @Column(name = "CUSTOMER_EMAIL")
    private String customerEmail;

    @Column(name = "DISCOUNT_BAND")
    private String discountBand;

    @Column(name = "DISCOUNT_PCT")
    private double discountPct;

    @Column(name = "POINTS_BALANCE")
    private int pointsBalance;

    @Column(name = "ISSUED_AT")
    private LocalDateTime issuedAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public String getTradeCardNumber() { return tradeCardNumber; }
    public void setTradeCardNumber(String v) { this.tradeCardNumber = v; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String v) { this.customerEmail = v; }
    public String getDiscountBand() { return discountBand; }
    public void setDiscountBand(String v) { this.discountBand = v; }
    public double getDiscountPct() { return discountPct; }
    public void setDiscountPct(double v) { this.discountPct = v; }
    public int getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(int v) { this.pointsBalance = v; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime v) { this.issuedAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}

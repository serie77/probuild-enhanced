package com.probuild.platform.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A stock line — a product (sold) or a tool (hired). */
@Entity
@Table(name = "INVENTORY")
public class Inventory {
    @Id @Column(name = "PRODUCT_CODE") public String productCode;
    @Column(name = "NAME") public String name;
    @Column(name = "PRICE") public double price;
    @Column(name = "PRODUCT_TYPE") public String productType; // PRODUCT | TOOL
    @Column(name = "STOCK_LEVEL") public int stockLevel;
    @Column(name = "BIN_LOCATION") public String binLocation;

    public Inventory() { }
    public Inventory(String code, String name, double price, String type, int stock, String bin) {
        this.productCode = code; this.name = name; this.price = price;
        this.productType = type; this.stockLevel = stock; this.binLocation = bin;
    }
}

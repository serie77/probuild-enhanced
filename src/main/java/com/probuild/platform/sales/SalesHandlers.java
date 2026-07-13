package com.probuild.platform.sales;

import com.probuild.platform.data.entity.Inventory;
import com.probuild.platform.data.repo.InventoryRepository;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** Job workers for the Sales capability (Cap_Sales): inventory, pricing, quoting, backorder. */
@Component
public class SalesHandlers {

    private static final Logger log = LoggerFactory.getLogger(SalesHandlers.class);
    private final InventoryRepository inventory;

    public SalesHandlers(InventoryRepository inventory) {
        this.inventory = inventory;
    }

    private static String ref(String prefix) {
        return prefix + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }

    @JobWorker(type = "sales-check-inventory", fetchAllVariables = true)
    public Map<String, Object> checkInventory(
            @Variable(name = "productCode") String productCode,
            @Variable(name = "quantityRequired") Integer quantityRequired) {
        int qty = quantityRequired != null ? quantityRequired : 1;
        Inventory inv = productCode != null ? inventory.findById(productCode).orElse(null) : null;
        boolean valid = inv != null && "PRODUCT".equalsIgnoreCase(inv.productType);
        boolean inStock = valid && inv.stockLevel >= qty;
        double unitPrice = valid ? inv.price : 0.0;

        Map<String, Object> out = new HashMap<>();
        out.put("inStock", inStock);
        out.put("stockStatus", inStock ? "In Stock" : "Out of Stock");
        out.put("productName", valid ? inv.name : productCode);
        out.put("unitPrice", unitPrice);
        out.put("binLocation", valid ? inv.binLocation : "");
        out.put("quantityRequired", qty);
        out.put("quotedPrice", Math.round(unitPrice * qty * 100.0) / 100.0);
        log.info("[sales-check-inventory] {} x{} inStock={} price={}", productCode, qty, inStock, out.get("quotedPrice"));
        return out;
    }

    @JobWorker(type = "sales-prepare-quote", fetchAllVariables = true)
    public Map<String, Object> prepareQuote(@Variable(name = "quotedPrice") Double quotedPrice) {
        Map<String, Object> out = new HashMap<>();
        out.put("quoteReference", ref("QTE-"));
        out.put("salesOutcome", "QUOTED");
        log.info("[sales-prepare-quote] {} total={}", out.get("quoteReference"), quotedPrice);
        return out;
    }

    @JobWorker(type = "sales-raise-backorder", fetchAllVariables = true)
    public Map<String, Object> raiseBackorder(@Variable(name = "productCode") String productCode) {
        Map<String, Object> out = new HashMap<>();
        out.put("poNumber", ref("PO-"));
        out.put("supplierName", "Jewson");
        out.put("salesOutcome", "BACKORDER");
        log.info("[sales-raise-backorder] {} po={}", productCode, out.get("poNumber"));
        return out;
    }
}

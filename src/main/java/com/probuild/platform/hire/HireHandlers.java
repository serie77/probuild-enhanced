package com.probuild.platform.hire;

import com.probuild.platform.data.entity.Inventory;
import com.probuild.platform.data.entity.Rental;
import com.probuild.platform.data.repo.InventoryRepository;
import com.probuild.platform.data.repo.RentalRepository;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Job workers for the Tool Hire capability (Cap_Hire). */
@Component
public class HireHandlers {

    private static final Logger log = LoggerFactory.getLogger(HireHandlers.class);
    private final InventoryRepository inventory;
    private final RentalRepository rentals;

    public HireHandlers(InventoryRepository inventory, RentalRepository rentals) {
        this.inventory = inventory;
        this.rentals = rentals;
    }

    private static String ref(String prefix) {
        return prefix + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }

    @JobWorker(type = "hire-verify-availability", fetchAllVariables = true)
    public Map<String, Object> verify(@Variable(name = "toolCode") String toolCode) {
        Inventory inv = toolCode != null ? inventory.findById(toolCode).orElse(null) : null;
        boolean available = inv != null && "TOOL".equalsIgnoreCase(inv.productType) && inv.stockLevel > 0;
        Map<String, Object> out = new HashMap<>();
        out.put("toolAvailable", available);
        out.put("dailyRate", inv != null ? inv.price : 0.0);
        out.put("toolName", inv != null ? inv.name : toolCode);
        log.info("[hire-verify-availability] {} available={}", toolCode, available);
        return out;
    }

    @JobWorker(type = "hire-confirm-booking", fetchAllVariables = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> book(final ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        String toolCode = (String) v.get("toolCode");
        double days = v.get("rentalDays") instanceof Number n ? n.doubleValue() : 1.0;
        double dailyRate = v.get("dailyRate") instanceof Number n ? n.doubleValue() : 0.0;
        Map<String, Object> tier = (Map<String, Object>) v.get("hireTier");
        double mult = tier != null && tier.get("tierMultiplier") instanceof Number n ? n.doubleValue() : 1.0;
        String tierLabel = tier != null ? String.valueOf(tier.get("tierLabel")) : "1 day";

        double fee = Math.round(dailyRate * days * mult * 100.0) / 100.0;
        double deposit = Math.round(fee * 0.5 * 100.0) / 100.0;

        Rental r = new Rental();
        r.bookingReference = ref("BK-");
        r.journeyId = (String) v.get("journeyId");
        r.toolCode = toolCode;
        r.rentalDays = days;
        r.dailyRate = dailyRate;
        r.rentalFee = fee;
        r.depositAmount = deposit;
        r.tierLabel = tierLabel;
        r.status = "ISSUED";
        r.issuedAt = LocalDateTime.now();
        r.updatedAt = r.issuedAt;
        rentals.save(r);
        // issue the tool: decrement stock
        if (toolCode != null) inventory.findById(toolCode).ifPresent(inv -> { inv.stockLevel = Math.max(0, inv.stockLevel - 1); inventory.save(inv); });
        log.info("[hire-confirm-booking] {} fee=£{} deposit=£{} tier={}", r.bookingReference, fee, deposit, tierLabel);

        Map<String, Object> out = new HashMap<>();
        out.put("bookingReference", r.bookingReference);
        out.put("rentalFee", fee);
        out.put("depositAmount", deposit);
        out.put("tierLabel", tierLabel);
        out.put("hireOutcome", "BOOKED");
        return out;
    }

    @JobWorker(type = "hire-flag-unavailable", fetchAllVariables = true)
    public Map<String, Object> unavailable(@Variable(name = "toolCode") String toolCode) {
        log.info("[hire-flag-unavailable] {}", toolCode);
        Map<String, Object> out = new HashMap<>();
        out.put("hireOutcome", "UNAVAILABLE");
        return out;
    }
}

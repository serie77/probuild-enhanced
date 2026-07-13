package com.probuild.platform.warehouse;

import com.probuild.platform.data.entity.Inventory;
import com.probuild.platform.data.entity.OrderRecord;
import com.probuild.platform.data.entity.Shipment;
import com.probuild.platform.data.repo.InventoryRepository;
import com.probuild.platform.data.repo.OrderRepository;
import com.probuild.platform.data.repo.ShipmentRepository;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Job workers for the Warehouse capability (Cap_Warehouse): order creation and dispatch. */
@Component
public class WarehouseHandlers {

    private static final Logger log = LoggerFactory.getLogger(WarehouseHandlers.class);
    private final OrderRepository orders;
    private final ShipmentRepository shipments;
    private final InventoryRepository inventory;

    public WarehouseHandlers(OrderRepository orders, ShipmentRepository shipments, InventoryRepository inventory) {
        this.orders = orders;
        this.shipments = shipments;
        this.inventory = inventory;
    }

    private static String ref(String prefix) {
        return prefix + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }

    @JobWorker(type = "warehouse-create-order", fetchAllVariables = true)
    public Map<String, Object> createOrder(final ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        LocalDateTime now = LocalDateTime.now();
        OrderRecord o = new OrderRecord();
        o.orderReference = ref("ORD-");
        o.journeyId = (String) v.get("journeyId");
        o.customerName = (String) v.get("customerName");
        o.productCode = (String) v.get("productCode");
        o.quantity = v.get("quantityRequired") instanceof Number n ? n.intValue() : 1;
        o.quotedPrice = v.get("quotedPrice") instanceof Number n ? n.doubleValue() : 0.0;
        o.finalAmount = o.quotedPrice;
        o.status = "CONFIRMED";
        o.createdAt = now;
        o.updatedAt = now;
        orders.save(o);
        log.info("[warehouse-create-order] {} qty={} amount={}", o.orderReference, o.quantity, o.finalAmount);

        Map<String, Object> out = new HashMap<>();
        out.put("orderReference", o.orderReference);
        out.put("finalAmount", o.finalAmount);
        return out;
    }

    @JobWorker(type = "warehouse-dispatch", fetchAllVariables = true)
    public Map<String, Object> dispatch(
            @Variable(name = "orderReference") String orderReference,
            @Variable(name = "journeyId") String journeyId,
            @Variable(name = "productCode") String productCode,
            @Variable(name = "quantityRequired") Integer quantityRequired,
            @Variable(name = "deliveryMethod") String deliveryMethod) {
        LocalDateTime now = LocalDateTime.now();
        String tracking = ref("TRK-");

        // decrement stock
        if (productCode != null) {
            inventory.findById(productCode).ifPresent(inv -> {
                inv.stockLevel = Math.max(0, inv.stockLevel - (quantityRequired != null ? quantityRequired : 1));
                inventory.save(inv);
            });
        }
        // shipment
        Shipment s = new Shipment();
        s.trackingNumber = tracking;
        s.orderReference = orderReference;
        s.journeyId = journeyId;
        s.direction = "OUTBOUND";
        s.destination = deliveryMethod;
        s.sentAt = now;
        shipments.save(s);
        // order status
        if (orderReference != null) {
            orders.findById(orderReference).ifPresent(o -> {
                o.status = "DISPATCHED";
                o.trackingNumber = tracking;
                o.updatedAt = now;
                orders.save(o);
            });
        }
        log.info("[warehouse-dispatch] {} tracking={}", orderReference, tracking);

        Map<String, Object> out = new HashMap<>();
        out.put("trackingNumber", tracking);
        return out;
    }

    /** Return path: restock immediately if sound, otherwise flag for FixPro maintenance. */
    @JobWorker(type = "warehouse-assess-return", fetchAllVariables = true)
    public Map<String, Object> assessReturn(
            @Variable(name = "toolCode") String toolCode,
            @Variable(name = "needsMaintenance") Boolean needsMaintenance) {
        boolean needs = Boolean.TRUE.equals(needsMaintenance);
        if (!needs && toolCode != null) {
            inventory.findById(toolCode).ifPresent(inv -> { inv.stockLevel += 1; inventory.save(inv); });
        }
        log.info("[warehouse-assess-return] {} needsMaintenance={}", toolCode, needs);
        Map<String, Object> out = new HashMap<>();
        out.put("returnOutcome", needs ? "SERVICED" : "RESTOCKED");
        out.put("maintenanceToolCount", 1);
        return out;
    }
}

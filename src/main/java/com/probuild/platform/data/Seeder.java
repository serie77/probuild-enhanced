package com.probuild.platform.data;

import com.probuild.platform.data.entity.Inventory;
import com.probuild.platform.data.entity.TradeCard;
import com.probuild.platform.data.repo.InventoryRepository;
import com.probuild.platform.data.repo.TradeCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** Seeds reference data on first startup. Idempotent — only seeds empty tables. */
@Component
public class Seeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Seeder.class);
    private final TradeCardRepository tradeCards;
    private final InventoryRepository inventory;

    public Seeder(TradeCardRepository tradeCards, InventoryRepository inventory) {
        this.tradeCards = tradeCards;
        this.inventory = inventory;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tradeCards.count() == 0) {
            LocalDateTime now = LocalDateTime.now();
            tradeCards.saveAll(List.of(
                    card("TC-BRONZE01", "john.smith@example.com", "Bronze", 2.0, 340, now),
                    card("TC-SILVER01", "alice@builders.co.uk", "Silver", 5.0, 1250, now),
                    card("TC-GOLD01", "carl@contractors.co.uk", "Gold", 10.0, 2680, now)
            ));
        }
        if (inventory.count() == 0) {
            inventory.saveAll(List.of(
                    new Inventory("TIM-2x4", "Treated Pine 2x4", 8.50, "PRODUCT", 150, "B1-01"),
                    new Inventory("INS-50", "Fibreglass Insulation 50mm", 12.99, "PRODUCT", 80, "B1-02"),
                    new Inventory("PLB-12", "Plasterboard 12.5mm", 7.45, "PRODUCT", 120, "B1-03"),
                    new Inventory("PWR-D18", "Power Drill Bits Set", 24.99, "PRODUCT", 35, "B2-01"),
                    new Inventory("FST-BOX", "Fasteners Assortment Box", 15.50, "PRODUCT", 200, "B2-02"),
                    new Inventory("ADH-PU", "Polyurethane Adhesive 500ml", 18.75, "PRODUCT", 95, "B2-03"),
                    new Inventory("MIX-110", "Cement Mixer 110L", 18.00, "TOOL", 4, "T1-01"),
                    new Inventory("SCF-T6", "Scaffold Tower 6m", 30.00, "TOOL", 3, "T1-02"),
                    new Inventory("PWH-2500", "Power Washer 2500psi", 25.00, "TOOL", 6, "T2-01"),
                    new Inventory("TCT-100", "Table Saw TCT 100mm", 20.00, "TOOL", 5, "T2-02"),
                    new Inventory("HDD-SDS", "Hammer Drill SDS Plus", 18.00, "TOOL", 8, "T2-03")
            ));
        }
        log.info("[seed] tradeCards={} inventory={}", tradeCards.count(), inventory.count());
    }

    private static TradeCard card(String no, String email, String band, double pct, int points, LocalDateTime now) {
        TradeCard c = new TradeCard();
        c.setTradeCardNumber(no);
        c.setCustomerEmail(email);
        c.setDiscountBand(band);
        c.setDiscountPct(pct);
        c.setPointsBalance(points);
        c.setIssuedAt(now);
        c.setUpdatedAt(now);
        return c;
    }
}

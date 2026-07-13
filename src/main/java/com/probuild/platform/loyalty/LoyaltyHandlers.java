package com.probuild.platform.loyalty;

import com.probuild.platform.data.entity.TradeCard;
import com.probuild.platform.data.repo.TradeCardRepository;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Job workers for the Loyalty capability process (Cap_Loyalty). */
@Component
public class LoyaltyHandlers {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyHandlers.class);
    private static final int POINTS_PER_POUND = 5;

    private final TradeCardRepository cards;

    public LoyaltyHandlers(TradeCardRepository cards) {
        this.cards = cards;
    }

    private static String ref(String prefix) {
        return prefix + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }

    @JobWorker(type = "loyalty-issue-card", fetchAllVariables = true)
    public Map<String, Object> issueCard(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String email = (String) vars.get("customerEmail");
        LocalDateTime now = LocalDateTime.now();

        TradeCard c = new TradeCard();
        c.setTradeCardNumber(ref("TC-"));
        c.setCustomerEmail(email);
        c.setDiscountBand("Bronze");
        c.setDiscountPct(2.0);
        c.setPointsBalance(0);
        c.setIssuedAt(now);
        c.setUpdatedAt(now);
        cards.save(c);
        log.info("[loyalty-issue-card] issued {} for {}", c.getTradeCardNumber(), email);

        Map<String, Object> out = new HashMap<>();
        out.put("tradeCardNumber", c.getTradeCardNumber());
        out.put("pointsBalance", 0);
        out.put("hasCard", true);
        return out;
    }

    @JobWorker(type = "loyalty-renew-card", fetchAllVariables = true)
    public Map<String, Object> renewCard(@Variable(name = "tradeCardNumber") String tradeCardNumber) {
        Map<String, Object> out = new HashMap<>();
        int balance = 0;
        if (tradeCardNumber != null) {
            TradeCard c = cards.findById(tradeCardNumber).orElse(null);
            if (c != null) {
                c.setUpdatedAt(LocalDateTime.now());
                cards.save(c);
                balance = c.getPointsBalance();
            }
        }
        log.info("[loyalty-renew-card] renewed {} balance={}", tradeCardNumber, balance);
        out.put("pointsBalance", balance);
        out.put("hasCard", tradeCardNumber != null && !tradeCardNumber.isBlank());
        return out;
    }

    @JobWorker(type = "loyalty-accrue-points", fetchAllVariables = true)
    public Map<String, Object> accruePoints(
            @Variable(name = "tradeCardNumber") String tradeCardNumber,
            @Variable(name = "quotedPrice") Double quotedPrice) {
        Map<String, Object> out = new HashMap<>();
        int balance = 0;
        boolean has = tradeCardNumber != null && !tradeCardNumber.isBlank();
        if (has) {
            TradeCard c = cards.findById(tradeCardNumber).orElse(null);
            if (c != null) {
                int earned = (int) Math.floor((quotedPrice != null ? quotedPrice : 0.0) * POINTS_PER_POUND);
                c.setPointsBalance(c.getPointsBalance() + earned);
                c.setUpdatedAt(LocalDateTime.now());
                cards.save(c);
                balance = c.getPointsBalance();
                log.info("[loyalty-accrue-points] {} +{} -> {}", tradeCardNumber, earned, balance);
            }
        }
        out.put("pointsBalance", balance);
        out.put("hasCard", has);
        return out;
    }

    /** Persists the DMN-decided band/percentage onto the card. */
    @JobWorker(type = "loyalty-persist-band", fetchAllVariables = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> persistBand(
            @Variable(name = "tradeCardNumber") String tradeCardNumber,
            @Variable(name = "bandResult") Map<String, Object> bandResult) {
        String band = bandResult != null ? String.valueOf(bandResult.get("discountBand")) : "None";
        double pct = bandResult != null && bandResult.get("discountPct") instanceof Number n ? n.doubleValue() : 0.0;

        if (tradeCardNumber != null) {
            cards.findById(tradeCardNumber).ifPresent(c -> {
                c.setDiscountBand(band);
                c.setDiscountPct(pct);
                c.setUpdatedAt(LocalDateTime.now());
                cards.save(c);
            });
        }
        log.info("[loyalty-persist-band] {} band={} pct={}", tradeCardNumber, band, pct);

        Map<String, Object> out = new HashMap<>();
        out.put("discountBand", band);
        out.put("discountPct", pct);
        return out;
    }
}

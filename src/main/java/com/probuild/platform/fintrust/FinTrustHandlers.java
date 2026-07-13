package com.probuild.platform.fintrust;

import com.probuild.platform.data.entity.FinanceTransfer;
import com.probuild.platform.data.repo.FinanceTransferRepository;
import com.probuild.platform.shared.Messages;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Job workers for the FinTrust partner (Partner_FinTrust). */
@Component
public class FinTrustHandlers {

    private static final Logger log = LoggerFactory.getLogger(FinTrustHandlers.class);
    private final FinanceTransferRepository transfers;
    private final Messages messages;

    public FinTrustHandlers(FinanceTransferRepository transfers, Messages messages) {
        this.transfers = transfers;
        this.messages = messages;
    }

    private static String ref(String prefix) {
        return prefix + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }

    @SuppressWarnings("unchecked")
    private static int term(Map<String, Object> financeResult) {
        if (financeResult != null && financeResult.get("termMonths") instanceof Number n) return n.intValue();
        return 6;
    }

    @JobWorker(type = "finance-transfer", fetchAllVariables = true)
    public Map<String, Object> transfer(final ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        Map<String, Object> financeResult = (Map<String, Object>) v.get("financeResult");
        int termMonths = term(financeResult);
        double amount = v.get("financeAmount") instanceof Number n ? n.doubleValue() : 0.0;

        FinanceTransfer ft = new FinanceTransfer();
        ft.transferReference = ref("FIN-");
        ft.journeyId = (String) v.get("journeyId");
        ft.customerName = (String) v.get("customerName");
        ft.amount = amount;
        ft.termMonths = termMonths;
        ft.status = "APPROVED";
        ft.recommendation = financeResult != null ? String.valueOf(financeResult.get("financeRecommendation")) : null;
        ft.creditScore = v.get("creditScore") instanceof Number n ? n.intValue() : null;
        ft.decidedAt = LocalDateTime.now();
        transfers.save(ft);
        log.info("[finance-transfer] {} £{} ({}m) rec={}", ft.transferReference, amount, termMonths, ft.recommendation);

        Map<String, Object> out = new HashMap<>();
        out.put("transferReference", ft.transferReference);
        out.put("financeTermMonths", termMonths);
        return out;
    }

    @JobWorker(type = "finance-reject", fetchAllVariables = true)
    public Map<String, Object> reject(final ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        Map<String, Object> financeResult = (Map<String, Object>) v.get("financeResult");
        FinanceTransfer ft = new FinanceTransfer();
        ft.transferReference = ref("FIN-REJ-");
        ft.journeyId = (String) v.get("journeyId");
        ft.customerName = (String) v.get("customerName");
        ft.amount = v.get("financeAmount") instanceof Number n ? n.doubleValue() : 0.0;
        ft.termMonths = 0;
        ft.status = "REJECTED";
        ft.recommendation = financeResult != null ? String.valueOf(financeResult.get("financeRecommendation")) : null;
        ft.creditScore = v.get("creditScore") instanceof Number n ? n.intValue() : null;
        ft.decidedAt = LocalDateTime.now();
        transfers.save(ft);
        log.info("[finance-reject] {} rec={}", ft.transferReference, ft.recommendation);

        Map<String, Object> out = new HashMap<>();
        out.put("transferReference", ft.transferReference);
        return out;
    }

    /** Publishes finance.settled back to the waiting orchestrator, carrying the decision. */
    @JobWorker(type = "publish-finance-settled", fetchAllVariables = true)
    public void settle(
            @Variable(name = "journeyId") String journeyId,
            @Variable(name = "financeApproved") Boolean financeApproved,
            @Variable(name = "financeTermMonths") Integer financeTermMonths,
            @Variable(name = "transferReference") String transferReference) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("financeApproved", Boolean.TRUE.equals(financeApproved));
        payload.put("financeTermMonths", financeTermMonths);
        payload.put("transferReference", transferReference);
        messages.publish("finance.settled", journeyId, payload);
        log.info("[publish-finance-settled] journeyId={} approved={}", journeyId, financeApproved);
    }
}

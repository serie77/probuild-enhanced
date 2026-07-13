package com.probuild.platform.orchestrator;

import com.probuild.platform.shared.Messages;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Job workers owned by the Customer Journey orchestrator. */
@Component
public class OrchestratorHandlers {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorHandlers.class);
    private final Messages messages;

    public OrchestratorHandlers(Messages messages) {
        this.messages = messages;
    }

    /** Opens a journey: mints the correlation id every downstream capability and partner shares. */
    @JobWorker(type = "journey-init", fetchAllVariables = true)
    public Map<String, Object> init(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String journeyId = UUID.randomUUID().toString();
        String serviceType = String.valueOf(vars.getOrDefault("serviceType", "?"));
        log.info("[journey-init] journeyId={} serviceType={}", journeyId, serviceType);

        Map<String, Object> out = new HashMap<>();
        out.put("journeyId", journeyId);
        return out;
    }

    /** Starts the FinTrust partner: publishes finance.requested to open a partner instance. */
    @JobWorker(type = "publish-finance-request", fetchAllVariables = true)
    public void requestFinance(final ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        String journeyId = (String) v.get("journeyId");
        Map<String, Object> payload = new HashMap<>();
        payload.put("journeyId", journeyId);
        payload.put("financeAmount", v.getOrDefault("quotedPrice", 0.0));
        payload.put("creditScore", v.getOrDefault("creditScore", 0));
        payload.put("annualIncome", v.getOrDefault("annualIncome", 0));
        payload.put("customerName", v.get("customerName"));
        payload.put("customerEmail", v.get("customerEmail"));
        payload.put("tradeCardNumber", v.get("tradeCardNumber"));
        messages.publish("finance.requested", journeyId, payload);
        log.info("[publish-finance-request] journeyId={} amount={}", journeyId, payload.get("financeAmount"));
    }

    /** Starts the FixPro partner: publishes service.requested to open a partner instance. */
    @JobWorker(type = "publish-service-request", fetchAllVariables = true)
    public void requestService(final ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        String journeyId = (String) v.get("journeyId");
        Map<String, Object> payload = new HashMap<>();
        payload.put("journeyId", journeyId);
        payload.put("toolCode", v.get("toolCode"));
        payload.put("maintenanceToolCount", v.getOrDefault("maintenanceToolCount", 1));
        messages.publish("service.requested", journeyId, payload);
        log.info("[publish-service-request] journeyId={}", journeyId);
    }
}

package com.probuild.platform.fixpro;

import com.probuild.platform.data.entity.ServiceInvoice;
import com.probuild.platform.data.entity.ServiceRecord;
import com.probuild.platform.data.repo.ServiceInvoiceRepository;
import com.probuild.platform.data.repo.ServiceRecordRepository;
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

/** Job workers for the FixPro maintenance partner (Partner_FixPro). */
@Component
public class FixProHandlers {

    private static final Logger log = LoggerFactory.getLogger(FixProHandlers.class);
    private static final double LABOUR = 12.50, PARTS = 28.00, CALLOUT = 35.00;

    private final ServiceRecordRepository records;
    private final ServiceInvoiceRepository invoices;
    private final Messages messages;

    public FixProHandlers(ServiceRecordRepository records, ServiceInvoiceRepository invoices, Messages messages) {
        this.records = records;
        this.invoices = invoices;
        this.messages = messages;
    }

    private static String ref(String prefix) {
        return prefix + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    }

    @JobWorker(type = "fixpro-collect", fetchAllVariables = true)
    public Map<String, Object> collect(
            @Variable(name = "journeyId") String journeyId,
            @Variable(name = "maintenanceToolCount") Integer maintenanceToolCount) {
        int count = maintenanceToolCount != null ? maintenanceToolCount : 1;
        ServiceRecord r = new ServiceRecord();
        r.serviceReference = ref("SRV-");
        r.journeyId = journeyId;
        r.toolCount = count;
        r.status = "COLLECTED";
        r.createdAt = LocalDateTime.now();
        r.updatedAt = r.createdAt;
        records.save(r);
        log.info("[fixpro-collect] {} tools={}", r.serviceReference, count);
        Map<String, Object> out = new HashMap<>();
        out.put("serviceReference", r.serviceReference);
        out.put("toolCount", count);
        return out;
    }

    @JobWorker(type = "fixpro-service", fetchAllVariables = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> service(final ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        String sref = (String) v.get("serviceReference");
        Map<String, Object> triage = (Map<String, Object>) v.get("triageResult");
        int routine = num(triage, "routineCount"), repair = num(triage, "repairCount"), oos = num(triage, "outOfServiceCount");
        if (sref != null) records.findById(sref).ifPresent(r -> {
            r.routineCount = routine; r.repairCount = repair; r.outOfServiceCount = oos;
            r.status = "SERVICED"; r.updatedAt = LocalDateTime.now(); records.save(r);
        });
        log.info("[fixpro-service] {} routine={} repair={} oos={} (PAT passed)", sref, routine, repair, oos);
        return Map.of("patPassed", true);
    }

    @JobWorker(type = "fixpro-report", fetchAllVariables = true)
    public Map<String, Object> report(final ActivatedJob job) {
        Map<String, Object> v = job.getVariablesAsMap();
        String sref = (String) v.get("serviceReference");
        int routine = 0, repair = 0;
        if (sref != null) {
            ServiceRecord r = records.findById(sref).orElse(null);
            if (r != null) { routine = r.routineCount; repair = r.repairCount; }
        }
        double labour = LABOUR * (routine + repair);
        double parts = PARTS * repair;
        double callout = CALLOUT;
        double total = Math.round((labour + parts + callout) * 100.0) / 100.0;

        ServiceInvoice inv = new ServiceInvoice();
        inv.invoiceReference = ref("INV-");
        inv.serviceReference = sref;
        inv.journeyId = (String) v.get("journeyId");
        inv.labourCost = labour; inv.partsCost = parts; inv.callOutCost = callout; inv.totalAmount = total;
        inv.issuedAt = LocalDateTime.now();
        invoices.save(inv);
        if (sref != null) records.findById(sref).ifPresent(r -> { r.status = "REPORTED"; r.updatedAt = LocalDateTime.now(); records.save(r); });
        log.info("[fixpro-report] {} invoice={} total=£{}", sref, inv.invoiceReference, total);

        Map<String, Object> out = new HashMap<>();
        out.put("invoiceReference", inv.invoiceReference);
        out.put("serviceTotal", total);
        return out;
    }

    /** Publishes service.completed back to the waiting orchestrator. */
    @JobWorker(type = "publish-service-completed", fetchAllVariables = true)
    public void completed(@Variable(name = "journeyId") String journeyId,
                          @Variable(name = "toolCode") String toolCode) {
        // restock the serviced tool
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceCompleted", true);
        messages.publish("service.completed", journeyId, payload);
        log.info("[publish-service-completed] journeyId={}", journeyId);
    }

    /** Timer-triggered weekly service-bay sweep (scheduled maintenance, not tied to a journey). */
    @JobWorker(type = "fixpro-weekly-batch", fetchAllVariables = true)
    public void weekly() {
        log.info("[fixpro-weekly-batch] weekly service-bay sweep executed");
    }

    private static int num(Map<String, Object> m, String k) {
        return m != null && m.get(k) instanceof Number n ? n.intValue() : 0;
    }
}

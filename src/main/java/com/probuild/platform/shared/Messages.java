package com.probuild.platform.shared;

import io.camunda.client.CamundaClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Single choke point for partner-message publication. Every message between the
 * customer orchestrator and a partner pool (FinTrust, FixPro) is published here,
 * keyed by the journey's correlation id, with a bounded TTL.
 */
@Component
public class Messages {

    private static final Duration TTL = Duration.ofMinutes(30);
    private final CamundaClient client;

    public Messages(CamundaClient client) {
        this.client = client;
    }

    public void publish(String messageName, String correlationKey, Map<String, Object> variables) {
        client.newPublishMessageCommand()
                .messageName(messageName)
                .correlationKey(correlationKey)
                .variables(variables)
                .timeToLive(TTL)
                .send()
                .join();
    }
}

package io.casehub.examples.helpdesk.yaml;

import io.casehub.api.engine.YamlCaseHub;
import io.casehub.api.model.CaseDefinition;
import io.casehub.connectors.chat.model.ChatChannelRef;
import io.casehub.connectors.chat.model.ChatContent;
import io.casehub.connectors.chat.spi.ChatPlatform;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;

@ApplicationScoped
public class HelpdeskYamlCaseHub extends YamlCaseHub {

    @Inject
    ChatPlatform chatPlatform;

    public HelpdeskYamlCaseHub() {
        super("casehub/helpdesk-ticket.yaml");
    }

    @Override
    protected void augment(CaseDefinition definition) {
        var workers = definition.getWorkers();
        workers.removeIf(w -> "ticket-classifier".equals(w.name())
                || "notification-sender".equals(w.name()));

        workers.add(Worker.builder()
                .name("ticket-classifier")
                .capabilityName("classify-ticket")
                .function(input -> {
                    String subject = String.valueOf(input.getOrDefault("subject", "")).toLowerCase();
                    String category = "OTHER";
                    String priority = "MEDIUM";
                    if (subject.contains("laptop") || subject.contains("monitor")
                            || subject.contains("keyboard") || subject.contains("printer")
                            || subject.contains("dock") || subject.contains("wifi")) {
                        category = "HARDWARE";
                        priority = "HIGH";
                    } else if (subject.contains("password") || subject.contains("access")
                            || subject.contains("login") || subject.contains("vpn")
                            || subject.contains("membership") || subject.contains("jira")) {
                        category = "ACCESS";
                        priority = "LOW";
                    } else if (subject.contains("install") || subject.contains("update")
                            || subject.contains("license") || subject.contains("ide")
                            || subject.contains("docker") || subject.contains("slack")) {
                        category = "SOFTWARE";
                        priority = "MEDIUM";
                    }
                    return WorkerResult.of(Map.of("category", category, "priority", priority));
                })
                .build());

        workers.add(Worker.builder()
                .name("notification-sender")
                .capabilityName("notify-customer")
                .function(input -> {
                    String customerRef = (String) input.get("customerRef");
                    String message = (String) input.get("message");
                    chatPlatform.messaging().send(
                            new ChatChannelRef(customerRef), new ChatContent(message));
                    return WorkerResult.of(Map.of("notified", true));
                })
                .build());
    }
}

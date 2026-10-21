package io.casehub.examples.manor.web;

import io.casehub.examples.manor.agent.AgentInvocationService;
import io.casehub.examples.manor.agent.ManorConfig;
import io.casehub.examples.manor.agent.PipelineSituationClassifier;
import io.casehub.neocortex.caps.CapsEngine;
import io.casehub.neocortex.caps.engine.HaikuBiasValidator;
import io.casehub.neocortex.caps.engine.HaikuBiasValidator.AuditEntry;
import io.casehub.neocortex.caps.engine.HaikuBiasValidator.DivergenceResult;
import io.casehub.neocortex.caps.llm.InlineClassificationPromptBuilder;
import io.casehub.platform.agent.AgentSessionInit;
import io.casehub.platform.agent.session.ManagedSession;
import io.casehub.platform.agent.session.SessionLifecycleManager;
import io.casehub.platform.agent.session.SessionPipeline;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/manor")
public class BiasValidationResource {

    private static final Logger log = Logger.getLogger(BiasValidationResource.class);

    @Inject
    CapsEngine capsEngine;
    @Inject
    ManorConfig config;
    @Inject
    io.casehub.platform.agent.session.SessionPoolRegistry sessionPoolRegistry;
    @Inject
    io.casehub.examples.manor.agent.ClassificationModeRegistry modeRegistry;

    @jakarta.ws.rs.GET
    @Path("/classification-mode")
    @Produces("application/json")
    public Response getClassificationMode() {
        return Response.ok(Map.of("externalCharacters", modeRegistry.externalCharacters())).build();
    }

    @POST
    @Path("/bias-validation")
    @Produces("application/json")
    public Response runBiasValidation(@jakarta.ws.rs.QueryParam("apply") @jakarta.ws.rs.DefaultValue("false") boolean apply) {
        var classificationConfig = config.classification();
        if (!classificationConfig.auditEnabled() || classificationConfig.auditLogPath() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Classification audit log not configured"))
                    .build();
        }

        var auditPath = java.nio.file.Path.of(classificationConfig.auditLogPath());
        if (!java.nio.file.Files.exists(auditPath)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Audit log file not found: " + auditPath))
                    .build();
        }

        List<AuditEntry> allEntries;
        try {
            allEntries = HaikuBiasValidator.parseAuditFile(auditPath);
        } catch (java.io.IOException e) {
            return Response.serverError()
                    .entity(Map.of("error", "Failed to read audit log: " + e.getMessage()))
                    .build();
        }

        var inlineEntries = allEntries.stream()
                .filter(e -> "inline".equals(e.mode()))
                .toList();

        if (inlineEntries.isEmpty()) {
            return Response.ok(Map.of("status", "no_inline_entries",
                    "totalEntries", allEntries.size())).build();
        }

        var classifier = new PipelineSituationClassifier(capsEngine.topology());
        var promptBuilder = new InlineClassificationPromptBuilder(capsEngine.topology());

        var sessionManager = new SessionLifecycleManager(sessionPoolRegistry);
        var pipeline = new SessionPipeline(sessionManager, "claude",
                new io.casehub.platform.agent.gate.AimdAdmissionStrategy(4, 2, Duration.ofSeconds(10)),
                80_000);

        var characterIds = inlineEntries.stream()
                .map(AuditEntry::agentId)
                .distinct()
                .toList();
        for (var charId : characterIds) {
            pipeline.openSession("bias:" + charId, AgentSessionInit.of(classifier.systemPrompt()));
        }

        var perEntry = new ArrayList<Map<String, Object>>();
        var perCharacter = new LinkedHashMap<String, List<DivergenceResult>>();

        try {
            for (var entry : inlineEntries) {
                var trajectoryText = promptBuilder.renderTrajectoryText(entry.trajectory());
                var userPrompt = classifier.buildUserPrompt(trajectoryText,
                        Map.of("agentId", entry.agentId()));

                var raw = pipeline.executeSingle("bias:" + entry.agentId(),
                        session -> AgentInvocationService.querySessionRaw(
                                (ManagedSession) session, userPrompt, "bias:" + entry.agentId()),
                        Duration.ofSeconds(60));

                var haikuCtx = classifier.parseResponse(raw);
                var divergence = HaikuBiasValidator.computeDivergence(
                        entry.activations(), haikuCtx.activations());

                perEntry.add(Map.of(
                        "tick", entry.tick(),
                        "agentId", entry.agentId(),
                        "jaccardDivergence", divergence.jaccardDivergence(),
                        "meanConfidenceDelta", divergence.meanConfidenceDelta(),
                        "sharedNodes", divergence.sharedNodes(),
                        "inlineNodeCount", entry.activations().size(),
                        "haikuNodeCount", haikuCtx.activations().size()));

                perCharacter.computeIfAbsent(entry.agentId(), k -> new ArrayList<>())
                        .add(divergence);
            }
        } finally {
            pipeline.shutdown();
        }

        var summary = new LinkedHashMap<String, Object>();
        for (var e : perCharacter.entrySet()) {
            var results = e.getValue();
            double meanJaccard = results.stream()
                    .mapToDouble(DivergenceResult::jaccardDivergence).average().orElse(0.0);
            double meanConfDelta = results.stream()
                    .mapToDouble(DivergenceResult::meanConfidenceDelta).average().orElse(0.0);
            summary.put(e.getKey(), Map.of(
                    "entries", results.size(),
                    "meanJaccardDivergence", meanJaccard,
                    "meanConfidenceDelta", meanConfDelta,
                    "biasFlag", meanJaccard > 0.5));
        }

        var switched = new ArrayList<String>();
        if (apply) {
            for (var e : perCharacter.entrySet()) {
                var results = e.getValue();
                double meanJaccard = results.stream()
                        .mapToDouble(DivergenceResult::jaccardDivergence).average().orElse(0.0);
                if (meanJaccard > 0.5 && !modeRegistry.isExternal(e.getKey())) {
                    modeRegistry.addExternal(e.getKey());
                    switched.add(e.getKey());
                    log.infof("Bias-informed switch: %s → external (mean Jaccard %.3f)", e.getKey(), meanJaccard);
                }
            }
        }

        log.infof("Bias validation complete: %d entries, %d characters, %d switched",
                inlineEntries.size(), characterIds.size(), switched.size());

        var result = new LinkedHashMap<String, Object>();
        result.put("status", "complete");
        result.put("totalAuditEntries", allEntries.size());
        result.put("inlineEntries", inlineEntries.size());
        result.put("characters", characterIds);
        result.put("summary", summary);
        result.put("entries", perEntry);
        if (apply) {
            result.put("switchedToExternal", switched);
        }
        return Response.ok(result).build();
    }
}

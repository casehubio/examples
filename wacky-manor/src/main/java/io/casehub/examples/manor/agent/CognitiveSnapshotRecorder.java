package io.casehub.examples.manor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.neocortex.cognition.core.CognitionCore;
import io.casehub.neocortex.cognition.drive.DriveAxis;
import io.casehub.neocortex.cognition.drive.DriveProfile;
import io.casehub.neocortex.memory.mood.MoodState;
import org.jboss.logging.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CognitiveSnapshotRecorder {

    private static final Logger log = Logger.getLogger(CognitiveSnapshotRecorder.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final CognitionCore cognitionCore;
    private final String tenantId;
    private final int intervalTicks;
    private final BufferedWriter writer;
    private final Map<String, ObjectNode> previousSnapshots = new HashMap<>();
    private final Map<String, CharacterCognition> cognitions;

    public CognitiveSnapshotRecorder(CognitionCore cognitionCore,
                                     String tenantId,
                                     int intervalTicks,
                                     Path outputPath,
                                     Map<String, CharacterCognition> cognitions) throws IOException {
        this.cognitionCore = cognitionCore;
        this.tenantId = tenantId;
        this.intervalTicks = intervalTicks;
        this.cognitions = cognitions;
        Files.createDirectories(outputPath.getParent());
        this.writer = Files.newBufferedWriter(outputPath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.infof("Cognitive snapshot recorder started — interval=%d, output=%s", intervalTicks, outputPath);
    }

    public boolean shouldCapture(int tick) {
        return intervalTicks > 0 && tick > 0 && tick % intervalTicks == 0;
    }

    public void capture(int tick, List<String> activeAgentIds) {
        for (var agentId : activeAgentIds) {
            try {
                var snapshot = buildSnapshot(tick, agentId);
                writer.write(mapper.writeValueAsString(snapshot));
                writer.newLine();
                previousSnapshots.put(agentId, snapshot);
            } catch (Exception e) {
                log.warnf(e, "Failed to capture cognitive snapshot for %s at tick %d", agentId, tick);
            }
        }
        try {
            writer.flush();
        } catch (IOException e) {
            log.warn("Failed to flush snapshot output", e);
        }
    }

    private ObjectNode buildSnapshot(int tick, String agentId) {
        var node = mapper.createObjectNode();
        node.put("tick", tick);
        node.put("agentId", agentId);
        node.put("capturedAt", Instant.now().toString());

        writeMood(node, agentId);
        writeSystemDrives(node, agentId);
        writeCharacterDrives(node, agentId);
        writeGoals(node, agentId);
        writeDelta(node, agentId);

        return node;
    }

    private void writeMood(ObjectNode node, String agentId) {
        cognitionCore.mood().currentMood(agentId, tenantId).ifPresent(mood -> {
            var moodNode = mapper.createObjectNode();
            moodNode.put("pleasure", round(mood.pleasure()));
            moodNode.put("arousal", round(mood.arousal()));
            moodNode.put("dominance", round(mood.dominance()));
            if (mood.cause() != null) moodNode.put("cause", mood.cause());
            node.set("mood", moodNode);
        });
    }

    private void writeSystemDrives(ObjectNode node, String agentId) {
        cognitionCore.drives().currentDrives(agentId, tenantId).ifPresent(profile -> {
            var drivesNode = mapper.createObjectNode();
            for (var entry : profile.drives().entrySet()) {
                drivesNode.put(entry.getKey().name().toLowerCase(), round(entry.getValue().intensity()));
            }
            drivesNode.put("composite", round(profile.compositeMotivation()));
            if (profile.dominantDrive() != null) {
                drivesNode.put("dominant", profile.dominantDrive().name().toLowerCase());
            }
            node.set("systemDrives", drivesNode);
        });
    }

    private void writeCharacterDrives(ObjectNode node, String agentId) {
        var cognition = cognitions.get(agentId);
        if (cognition == null) return;
        var drives = cognition.resolveAdaptedDrives();
        if (drives.isEmpty()) return;

        var drivesNode = mapper.createObjectNode();
        for (var drive : drives) {
            drivesNode.put(drive.type(), round(drive.intensity()));
        }
        node.set("characterDrives", drivesNode);
    }

    private void writeGoals(ObjectNode node, String agentId) {
        if (cognitionCore.goals() == null) return;
        cognitionCore.goals().currentProposals(agentId, tenantId).ifPresent(proposals -> {
            if (proposals.isEmpty()) return;
            var goalsArray = mapper.createArrayNode();
            for (var proposal : proposals) {
                var goalNode = mapper.createObjectNode();
                goalNode.put("axis", proposal.axis().name().toLowerCase());
                goalNode.put("name", proposal.goalName());
                goalNode.put("driveIntensity", round(proposal.driveIntensity()));
                goalsArray.add(goalNode);
            }
            node.set("goals", goalsArray);
        });
    }

    private void writeDelta(ObjectNode node, String agentId) {
        var previous = previousSnapshots.get(agentId);
        if (previous == null) return;

        var delta = mapper.createObjectNode();
        boolean hasDelta = false;

        var prevMood = previous.get("mood");
        var currMood = node.get("mood");
        if (prevMood != null && currMood != null) {
            var moodDelta = mapper.createObjectNode();
            moodDelta.put("pleasure", round(currMood.get("pleasure").asDouble() - prevMood.get("pleasure").asDouble()));
            moodDelta.put("arousal", round(currMood.get("arousal").asDouble() - prevMood.get("arousal").asDouble()));
            moodDelta.put("dominance", round(currMood.get("dominance").asDouble() - prevMood.get("dominance").asDouble()));
            delta.set("mood", moodDelta);
            hasDelta = true;
        }

        var prevDrives = previous.get("characterDrives");
        var currDrives = node.get("characterDrives");
        if (prevDrives != null && currDrives != null) {
            var drivesDelta = mapper.createObjectNode();
            var fieldNames = currDrives.fieldNames();
            boolean anyChange = false;
            while (fieldNames.hasNext()) {
                var field = fieldNames.next();
                double curr = currDrives.get(field).asDouble();
                double prev = prevDrives.has(field) ? prevDrives.get(field).asDouble() : 0;
                double d = curr - prev;
                if (Math.abs(d) > 0.001) {
                    drivesDelta.put(field, round(d));
                    anyChange = true;
                }
            }
            if (anyChange) {
                delta.set("characterDrives", drivesDelta);
                hasDelta = true;
            }
        }

        if (hasDelta) {
            node.set("delta", delta);
        }
    }

    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            log.warn("Failed to close snapshot writer", e);
        }
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}

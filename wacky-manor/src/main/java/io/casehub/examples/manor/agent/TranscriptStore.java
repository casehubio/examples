package io.casehub.examples.manor.agent;

import io.casehub.neocortex.corpus.CorpusStore;
import io.casehub.neocortex.corpus.zip.CorpusConfig;
import io.casehub.neocortex.corpus.zip.ZipCorpusStore;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

public final class TranscriptStore {

    private static final Logger log = Logger.getLogger(TranscriptStore.class);

    private final CorpusStore store;
    private final String profileMode;

    public TranscriptStore(Path storageDir, String profileMode) {
        this.profileMode = profileMode;
        try {
            var corpusDir = storageDir.resolve(profileMode.toLowerCase());
            Files.createDirectories(corpusDir);
            var config = new CorpusConfig("transcripts", corpusDir);
            this.store = new ZipCorpusStore(config);
            log.infof("TranscriptStore initialised: %s", corpusDir);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise TranscriptStore at " + storageDir, e);
        }
    }

    public void appendTick(int tick, Map<String, String> prompts, Map<String, String> responses) {
        var timestamp = Instant.now().toString();
        for (var entry : prompts.entrySet()) {
            String agentId = entry.getKey();
            String prompt = entry.getValue();
            String response = responses.get(agentId);
            if (response == null) continue;

            String path = agentId + "/tick-" + String.format("%04d", tick) + ".json";
            String json = toJson(tick, agentId, timestamp, prompt, response);

            Thread.ofVirtual().name("transcript-" + agentId + "-" + tick).start(() -> {
                try {
                    store.append(path, json.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    log.warnf("Failed to persist transcript for %s tick %d: %s", agentId, tick, e.getMessage());
                }
            });
        }
    }

    static String toJson(int tick, String agentId, String timestamp, String prompt, String response) {
        return "{\"tick\":" + tick
                + ",\"agentId\":" + jsonString(agentId)
                + ",\"timestamp\":" + jsonString(timestamp)
                + ",\"prompt\":" + jsonString(prompt)
                + ",\"response\":" + jsonString(response)
                + "}";
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        var sb = new StringBuilder(value.length() + 16);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}

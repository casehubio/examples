package io.casehub.examples.manor.agent;

import io.casehub.neocortex.mindmap.MindMapNode;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.SubgraphTypes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class ObservationSegmentCache {

    private static final Logger LOG = Logger.getLogger(ObservationSegmentCache.class.getName());
    private static final String CONDENSED_TRAIT = "Condensed";

    private final MindMapStore store;
    private final String tenantId;
    private final int maxTokenBudget;
    private final int recentNodeLimit;
    private final Map<String, CachedSegment> cache = new ConcurrentHashMap<>();
    private final Map<String, String> subgraphIds = new ConcurrentHashMap<>();

    public ObservationSegmentCache(MindMapStore store, String tenantId,
                                   int maxTokenBudget, int recentNodeLimit) {
        this.store = store;
        this.tenantId = tenantId;
        this.maxTokenBudget = maxTokenBudget;
        this.recentNodeLimit = recentNodeLimit;
    }

    public String getHistory(String characterId) {
        resolveSubgraphId(characterId);
        var segment = cache.get(characterId);
        return segment != null ? segment.text : "";
    }

    public void refresh(String characterId) {
        String sgId = resolveSubgraphId(characterId);
        if (sgId == null) return;

        var allNodes = store.nodesIn(sgId, tenantId);
        if (allNodes.isEmpty()) {
            cache.remove(characterId);
            return;
        }

        var condensed = allNodes.stream()
                .filter(n -> n.traits().contains(CONDENSED_TRAIT))
                .sorted(Comparator.comparing(n -> n.validFrom() != null ? n.validFrom() : n.createdAt()))
                .toList();

        var recent = allNodes.stream()
                .filter(n -> !n.traits().contains(CONDENSED_TRAIT))
                .sorted(Comparator.comparing(n -> n.validFrom() != null ? n.validFrom() : n.createdAt()))
                .toList();

        int recentCount = Math.min(recentNodeLimit, recent.size());
        var recentWindow = recent.subList(recent.size() - recentCount, recent.size());

        var sb = new StringBuilder();
        int charBudget = maxTokenBudget * 4;

        if (!condensed.isEmpty()) {
            sb.append("Earlier:\n");
            for (var node : condensed) {
                String content = node.properties().getOrDefault("content", "");
                String rooms = node.properties().getOrDefault("rooms", "");
                if (!content.isBlank()) {
                    if (!rooms.isBlank()) {
                        sb.append("[").append(rooms).append("] ");
                    }
                    sb.append(content.length() > 200 ? content.substring(0, 200) + "..." : content);
                    sb.append("\n");
                }
                if (sb.length() > charBudget / 2) break;
            }
        }

        if (!recentWindow.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("Recently:\n");
            for (var node : recentWindow) {
                String content = node.properties().getOrDefault("content", "");
                String room = node.properties().getOrDefault("room", "");
                if (!content.isBlank()) {
                    if (!room.isBlank()) {
                        sb.append("[").append(room).append("] ");
                    }
                    sb.append(content);
                    sb.append("\n");
                }
                if (sb.length() > charBudget) break;
            }
        }

        String text = sb.toString().strip();
        if (text.isEmpty()) {
            cache.remove(characterId);
        } else {
            cache.put(characterId, new CachedSegment(text, allNodes.size()));
        }
    }

    public void refreshAll(Iterable<String> characterIds) {
        for (String id : characterIds) {
            refresh(id);
        }
    }

    private String resolveSubgraphId(String characterId) {
        return subgraphIds.computeIfAbsent(characterId, id -> {
            var subgraphs = store.listSubgraphs(tenantId);
            for (var sg : subgraphs) {
                if (SubgraphTypes.ACTIVITY.equals(sg.type())
                        && sg.name().equals("observations-" + id)) {
                    return sg.id();
                }
            }
            return null;
        });
    }

    record CachedSegment(String text, int nodeCount) {}
}

package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.TickSnapshot;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@ApplicationScoped
public class AssertionRegistry {

    private static final Logger log = Logger.getLogger(AssertionRegistry.class);

    private final Map<String, Predicate<TickSnapshot>> predicates = new LinkedHashMap<>();
    private final Map<String, List<Boolean>> history = new LinkedHashMap<>();

    public void register(String id, Predicate<TickSnapshot> predicate) {
        predicates.put(id, predicate);
        history.put(id, new ArrayList<>());
    }

    public void evaluate(TickSnapshot snapshot) {
        for (var entry : predicates.entrySet()) {
            boolean satisfied = entry.getValue().test(snapshot);
            history.get(entry.getKey()).add(satisfied);
            if (satisfied) {
                log.infof("ASSERTION %s satisfied at tick %d", entry.getKey(), snapshot.tick());
            }
        }
    }

    public boolean wasSatisfied(String id) {
        var results = history.get(id);
        return results != null && results.contains(true);
    }

    public int firstSatisfiedTick(String id) {
        var results = history.get(id);
        if (results == null) return -1;
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i)) return i + 1;
        }
        return -1;
    }

    public void clear() {
        predicates.clear();
        history.clear();
    }
}

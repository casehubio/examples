package io.casehub.examples.manor.agent;

import io.casehub.blocks.agentic.social.need.NeedTier;
import io.casehub.blocks.agentic.social.need.NeedTierMappingProvider;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Set;

import static io.casehub.blocks.agentic.social.need.NeedTier.*;
import static java.util.Map.entry;

@ApplicationScoped
public class ManorNeedTierMappingProvider implements NeedTierMappingProvider {

    @Override
    public Map<String, Set<NeedTier>> tierMapping() {
        return Map.ofEntries(
            entry("scheming",          Set.of(SELF_EXPRESSION)),
            entry("self-preservation", Set.of(SAFETY)),
            entry("dominance",         Set.of(SELF_EXPRESSION)),
            entry("curiosity",         Set.of(UNDERSTANDING)),
            entry("social-harmony",    Set.of(SOCIAL)),
            entry("adventure",         Set.of(UNDERSTANDING, SOCIAL)),
            entry("gallantry",         Set.of(SOCIAL, TASKS)),
            entry("proving-worth",     Set.of(TASKS, SELF_EXPRESSION)),
            entry("protection",        Set.of(SAFETY, SOCIAL)),
            entry("greed",             Set.of(SELF_EXPRESSION)),
            entry("recognition",       Set.of(SELF_EXPRESSION, SOCIAL)),
            entry("loyalty",           Set.of(SOCIAL, TASKS)),
            entry("suspicion",         Set.of(SAFETY))
        );
    }
}

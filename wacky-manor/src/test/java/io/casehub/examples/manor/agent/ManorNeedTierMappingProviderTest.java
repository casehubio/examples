package io.casehub.examples.manor.agent;

import io.casehub.blocks.agentic.social.need.NeedTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManorNeedTierMappingProviderTest {

    @Test
    void maps13DriveTypes() {
        var provider = new ManorNeedTierMappingProvider();
        var mapping = provider.tierMapping();
        assertEquals(13, mapping.size(), "Should map all 13 drive types");
    }

    @Test
    void schemingMapToSelfExpression() {
        var provider = new ManorNeedTierMappingProvider();
        var tiers = provider.tierMapping().get("scheming");
        assertNotNull(tiers);
        assertTrue(tiers.contains(NeedTier.SELF_EXPRESSION));
    }

    @Test
    void adventureMapsToMultipleTiers() {
        var provider = new ManorNeedTierMappingProvider();
        var tiers = provider.tierMapping().get("adventure");
        assertNotNull(tiers);
        assertEquals(2, tiers.size(), "adventure should map to 2 tiers");
        assertTrue(tiers.contains(NeedTier.UNDERSTANDING));
        assertTrue(tiers.contains(NeedTier.SOCIAL));
    }

    @Test
    void allTiersCovered() {
        var provider = new ManorNeedTierMappingProvider();
        var mapping = provider.tierMapping();
        var allTiers = new java.util.HashSet<NeedTier>();
        mapping.values().forEach(allTiers::addAll);
        for (NeedTier tier : NeedTier.values()) {
            assertTrue(allTiers.contains(tier),
                "Tier " + tier + " should be reachable by at least one drive");
        }
    }
}

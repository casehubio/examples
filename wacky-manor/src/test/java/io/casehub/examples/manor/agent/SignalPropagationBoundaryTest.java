package io.casehub.examples.manor.agent;

import io.casehub.agency.perception.observation.affordance.ObservationSection;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.OverlayRef;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for all 14 personality signal propagation paths.
 * Each test seeds data, renders through CharacterCognition, and asserts
 * the signal reaches the rendered prompt text.
 *
 * neocortex#791
 */
class SignalPropagationBoundaryTest {

    private static final String TENANT = "propagation-test";
    private static final String AGENT = "test-agent";

    // ── 1. Tendencies ──────────────────────────────────────────────────

    @Test
    void tendencies_renderedInPrompt() {
        var config = new SocialConfig(
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of("Always speak with authority", "Never show weakness"),
                List.of(), List.of(), null, List.of(), null, null);
        var cognition = new CharacterCognition(AGENT, null, null, config, List.of());
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var tendencies = findSection(sections, "Your Behavioral Tendencies");
        assertThat(tendencies).as("Tendencies section must exist").isNotNull();
        var items = ((ObservationSection.ItemList) tendencies).items();
        assertThat(items).contains("Always speak with authority", "Never show weakness");
    }

    // ── 2. Beliefs ─────────────────────────────────────────────────────

    @Test
    void beliefs_renderedFromMindMapInPrompt() {
        var store = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);

        var beliefs = List.of(
                new SocialConfig.InitialBelief("others", "People are inherently selfish"),
                new SocialConfig.InitialBelief("self", "I deserve recognition"));
        var config = new SocialConfig(
                List.of(), List.of(new SocialConfig.Drive("scheming", 0.5, "dummy")),
                List.of(), beliefs, List.of(), Map.of(), null,
                null, List.of(), List.of(), List.of(), null, List.of(), null, null);
        seeder.seed(AGENT, config, TENANT);

        var cognition = buildCognition(store, config, true);
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var beliefSection = findSection(sections, "Your Beliefs");
        assertThat(beliefSection).as("Beliefs section must exist").isNotNull();
        var items = ((ObservationSection.ItemList) beliefSection).items();
        assertThat(items).anyMatch(i -> i.contains("People are inherently selfish"));
        assertThat(items).anyMatch(i -> i.contains("I deserve recognition"));
    }

    // ── 3. Norms ───────────────────────────────────────────────────────

    @Test
    void norms_renderedInPrompt() {
        var config = new SocialConfig(
                List.of(), List.of(), List.of(
                    new SocialConfig.NormEntry("Never attack an unarmed opponent", 10),
                    new SocialConfig.NormEntry("Bow before entering", 5)),
                List.of(), List.of(), Map.of(), null,
                null, List.of(), List.of(), List.of(), null, List.of(), null, null);
        var cognition = new CharacterCognition(AGENT, null, null, config, List.of());
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var normsSection = findSection(sections, "Social Rules");
        assertThat(normsSection).as("Norms section must exist").isNotNull();
        var items = ((ObservationSection.ItemList) normsSection).items();
        assertThat(items).contains("Never attack an unarmed opponent");
        assertThat(items).contains("Bow before entering");
    }

    // ── 4. Personality facet origins (partial → complete) ──────────────

    @Test
    void personalityFacetOrigins_renderedWithScoresAndText() {
        var facets = List.of(
                new SocialConfig.PersonalityFacet("Manipulativeness", 85, "Learned early that charm opens doors"),
                new SocialConfig.PersonalityFacet("Callousness", 70, "Empathy pruned by childhood neglect"));
        var config = new SocialConfig(
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), facets, List.of(), null, List.of(), null, null);

        // Briefing mode: origin text rendered
        var briefing = new CharacterCognition(AGENT, null, null, config, List.of());
        var briefingSections = briefing.renderCognitiveSections(character(), List.of(), Map.of());
        var briefingWho = findSection(briefingSections, "Who You Are");
        assertThat(briefingWho).as("Briefing-mode 'Who You Are' must exist").isNotNull();
        var briefingText = ((ObservationSection.TextBlock) briefingWho).content();
        assertThat(briefingText).contains("Learned early that charm opens doors");
        assertThat(briefingText).contains("Empathy pruned by childhood neglect");

        // Non-briefing mode: structured scores rendered
        var store = new InMemoryMindMapStore();
        var nonBriefing = buildCognition(store, config, false);
        var nbSections = nonBriefing.renderCognitiveSections(character(), List.of(), Map.of());
        var nbWho = findSection(nbSections, "Who You Are");
        assertThat(nbWho).as("Non-briefing 'Who You Are' must exist").isNotNull();
        var items = ((ObservationSection.ItemList) nbWho).items();
        assertThat(items).contains("Manipulativeness: 85/100");
        assertThat(items).contains("Callousness: 70/100");
    }

    // ── 5. Attachment style (zero coverage) ────────────────────────────

    @Test
    void attachmentStyle_renderedInObservation() {
        var attachment = new SocialConfig.AttachmentStyle("dismissive-avoidant", 0.2, 0.8);
        var config = new SocialConfig(
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), List.of(), List.of(), attachment, List.of(), null, null);
        var cognition = new CharacterCognition(AGENT, null, null, config, List.of());
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var bondSection = findSection(sections, "How You Bond");
        assertThat(bondSection).as("'How You Bond' section must exist for attachment style").isNotNull();
        var content = ((ObservationSection.TextBlock) bondSection).content();
        assertThat(content).contains("don't need people");
    }

    // ── 6. AMPD facet scores (zero coverage) ──────────────────────────

    @Test
    void ampdFacetScores_renderedInObservation() {
        var facets = List.of(
                new SocialConfig.PersonalityFacet("Hostility", 92, "origin text"),
                new SocialConfig.PersonalityFacet("Deceitfulness", 78, "origin text"));
        var config = new SocialConfig(
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), facets, List.of(), null, List.of(), null, null);

        var store = new InMemoryMindMapStore();
        var cognition = buildCognition(store, config, false);
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var whoSection = findSection(sections, "Who You Are");
        assertThat(whoSection).as("Non-briefing 'Who You Are' must exist").isNotNull();
        assertThat(whoSection).isInstanceOf(ObservationSection.ItemList.class);
        var items = ((ObservationSection.ItemList) whoSection).items();
        assertThat(items).contains("Hostility: 92/100");
        assertThat(items).contains("Deceitfulness: 78/100");
    }

    // ── 7. Drives ──────────────────────────────────────────────────────

    @Test
    void drives_renderedInPrompt() {
        var store = new InMemoryMindMapStore();
        var sgId = store.createSubgraph(
                new SubgraphInput("cognitive-" + AGENT, SubgraphTypes.COGNITIVE, null), TENANT);

        seedDriveNode(store, sgId, "scheming", 0.9);
        seedDriveNode(store, sgId, "gallantry", 0.7);

        var config = new SocialConfig(
                List.of(), List.of(
                    new SocialConfig.Drive("scheming", 0.9, "Plans within plans"),
                    new SocialConfig.Drive("gallantry", 0.7, "Chivalrous instinct")),
                List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), List.of(), List.of(), null, List.of(), null, null);
        var cognition = buildCognition(store, config, true);
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var whatPulls = findSection(sections, "What Pulls at You");
        assertThat(whatPulls).as("'What Pulls at You' section must exist").isNotNull();
        var content = ((ObservationSection.TextBlock) whatPulls).content();
        assertThat(content).containsIgnoringCase("SCHEMING");
        assertThat(content).containsIgnoringCase("GALLANTRY");
        assertThat(content).contains("0.9");
        assertThat(content).contains("0.7");
    }

    // ── 8. Formation memories ──────────────────────────────────────────

    @Test
    void formationMemories_renderedInPrompt() {
        var memories = List.of(
                new SocialConfig.FormationMemory(8, "The day the teacher said you would never amount to anything", -0.7, 0.8, 0.2),
                new SocialConfig.FormationMemory(14, "Winning the championship against all odds", 0.9, 0.7, 0.8));
        var config = new SocialConfig(
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), List.of(), List.of(), null, memories, null, null);
        var cognition = new CharacterCognition(AGENT, null, null, config, List.of());
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var memSection = findSection(sections, "Where You Come From");
        assertThat(memSection).as("'Where You Come From' section must exist").isNotNull();
        var content = ((ObservationSection.TextBlock) memSection).content();
        assertThat(content).contains("teacher said you would never amount");
        assertThat(content).contains("Winning the championship");
    }

    // ── 9. Goals ───────────────────────────────────────────────────────

    @Test
    void goals_renderedInPrompt() {
        var store = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var goals = List.of(
                new SocialConfig.GoalConfig("Dominate the manor", "Take control of every room", "COMPETENCE", 0.9, "Born to rule"));
        var drives = List.of(new SocialConfig.Drive("dominance", 0.9, "Control everything"));
        var config = new SocialConfig(
                goals, drives, List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), List.of(), List.of(), null, List.of(), null, null);
        var seedResult = seeder.seed(AGENT, config, TENANT);

        var goalOrchestrator = new io.casehub.neocortex.cognition.goal.GoalProposalOrchestrator(
                null, List.of(), null, java.util.Optional.empty(),
                null, null, null,
                io.casehub.neocortex.cognition.goal.GoalProposalConfig.defaults(),
                io.casehub.neocortex.cognition.goal.GoalEscalationConfig.defaults(),
                java.time.Clock.systemUTC());
        seeder.seedGoals(AGENT, config, goalOrchestrator, TENANT);

        var cognitionCore = new io.casehub.neocortex.cognition.core.CognitionCore(
                null, null, null, null, null, null, goalOrchestrator, null, null,
                io.casehub.neocortex.cognition.core.CognitionConfig.none().with("goals", true),
                store, new ManorNeedTierMappingProvider(), null, null, null, null, null, null);

        var cognition = new CharacterCognition(
                AGENT, null, null, config, List.of(),
                null, new ManorContextStrategy(), cognitionCore, seedResult,
                TENANT, store, null, null, true);
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var goalsSection = findSection(sections, "Your current goals:");
        assertThat(goalsSection).as("Goals section must exist").isNotNull();
        var content = ((ObservationSection.TextBlock) goalsSection).content();
        assertThat(content).contains("Take control of every room");
    }

    // ── 10. Relational schemas from config (zero coverage) ─────────────

    @Test
    void relationalSchemas_renderedForNearbyAgents() {
        var schemas = List.of(
                new SocialConfig.RelationalSchema("villain-a", "rival", -0.5, 0.1, 0.3,
                        "They stand between you and everything you deserve"),
                new SocialConfig.RelationalSchema("ally-b", "ally", 0.7, 0.5, 0.8,
                        "Your most trusted confederate"));
        var config = new SocialConfig(
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), List.of(), schemas, null, List.of(), null, null);
        var cognition = new CharacterCognition(AGENT, null, null, config, List.of());
        var sections = cognition.renderCognitiveSections(
                character(), List.of("villain-a"), Map.of("villain-a", "The Villain"));

        var feelSection = findSection(sections, "How You Feel About Those Present");
        assertThat(feelSection).as("'How You Feel' section must exist when nearby agent has schema").isNotNull();
        var content = ((ObservationSection.TextBlock) feelSection).content();
        assertThat(content).contains("The Villain");
        assertThat(content).contains("everything you deserve");
    }

    // ── 11. CAPS attractors ────────────────────────────────────────────

    @Test
    void capsAttractors_renderedInPrompt() {
        var store = new InMemoryMindMapStore();
        var cogSgId = store.createSubgraph(
                new SubgraphInput("cognitive-" + AGENT, SubgraphTypes.COGNITIVE, null), TENANT);
        var behSgId = store.createSubgraph(
                new SubgraphInput("behavioral-" + AGENT, SubgraphTypes.BEHAVIORAL, null), TENANT);

        seedDriveNode(store, cogSgId, "dominance", 0.8);

        store.addNode(NodeInput.of("attractor-dominance-control", behSgId)
                .withTraits(Set.of("CapsGenerated"))
                .withProperties(Map.of(
                        "agent-id", AGENT,
                        "strength", "0.75",
                        "personality-label", "controlling-dominance pattern")), TENANT);

        var config = new SocialConfig(
                List.of(), List.of(new SocialConfig.Drive("dominance", 0.8, "Control")),
                List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), List.of(), List.of(), null, List.of(), null, null);
        var cognition = buildCognition(store, config, true);
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var whatPulls = findSection(sections, "What Pulls at You");
        assertThat(whatPulls).as("'What Pulls at You' must exist").isNotNull();
        var content = ((ObservationSection.TextBlock) whatPulls).content();
        assertThat(content).contains("controlling-dominance pattern");
    }

    // ── 12. Disposition axes → CAPS attractors (intermediate → e2e) ───

    @Test
    void dispositionAxes_influenceCapsAttractorsInPrompt() {
        var store = new InMemoryMindMapStore();
        var cogSgId = store.createSubgraph(
                new SubgraphInput("cognitive-" + AGENT, SubgraphTypes.COGNITIVE, null), TENANT);

        seedDriveNode(store, cogSgId, "dominance", 0.9);

        var disposition = new SocialConfig.Disposition(
                "competitive", "flexible", "bold", "high", "competitive");
        var config = new SocialConfig(
                List.of(), List.of(new SocialConfig.Drive("dominance", 0.9, "Absolute control")),
                List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), List.of(), List.of(), null, List.of(), null, disposition);

        // Initialize CAPS with disposition and settle
        var capsEngine = new io.casehub.neocortex.caps.testing.InMemoryCapsEngine();
        new ManorCognitiveSeeder(store).seedCapsState(AGENT, config, TENANT, capsEngine);
        var state = capsEngine.loadState(TENANT, AGENT);
        assertThat(state).as("CAPS state must be initialized from disposition").isNotNull();

        var settling = capsEngine.settle(state, Map.of("dominance", 0.9));
        assertThat(settling.attractors()).as("Settling must produce attractors").isNotEmpty();

        // Seed attractors into behavioral subgraph (simulating BehavioralSynthesisPhase)
        var behSgId = store.createSubgraph(
                new SubgraphInput("behavioral-" + AGENT, SubgraphTypes.BEHAVIORAL, null), TENANT);
        for (var attractor : settling.attractors()) {
            if (attractor.strength() > 0.1) {
                var props = new java.util.HashMap<String, String>();
                props.put("agent-id", AGENT);
                props.put("strength", String.valueOf(attractor.strength()));
                if (attractor.personalityLabel() != null) {
                    props.put("personality-label", attractor.personalityLabel());
                }
                store.addNode(NodeInput.of(attractor.nodeId(), behSgId)
                        .withTraits(Set.of("CapsGenerated"))
                        .withProperties(Map.copyOf(props)), TENANT);
            }
        }

        var cognition = buildCognition(store, config, true);
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var whatPulls = findSection(sections, "What Pulls at You");
        assertThat(whatPulls).as("'What Pulls at You' must exist with CAPS attractors").isNotNull();
        var content = ((ObservationSection.TextBlock) whatPulls).content();
        assertThat(content).as("Disposition-influenced attractor text must reach prompt")
                .containsAnyOf("Something deep-rooted", "Something", "A faint thread");
    }

    // ── 13. Drive adaptation (intermediate → e2e) ─────────────────────

    @Test
    void driveAdaptation_adaptedIntensitiesReachPrompt() {
        var store = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);

        var config = new SocialConfig(
                List.of(), List.of(
                    new SocialConfig.Drive("scheming", 0.9, "Plans within plans"),
                    new SocialConfig.Drive("gallantry", 0.7, "Chivalrous")),
                List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), List.of(), List.of(), null, List.of(), null, null);
        seeder.seed(AGENT, config, TENANT);

        // Simulate drive adaptation: reduce scheming from 0.9 to 0.4
        var subgraphs = store.listSubgraphs(TENANT);
        var cognitiveSg = subgraphs.stream()
                .filter(s -> "cognitive".equals(s.type())).findFirst().orElseThrow();
        store.nodesIn(cognitiveSg.id(), TENANT).stream()
                .filter(n -> "drive-intensity".equals(n.properties().get("cognitiveKind")))
                .filter(n -> AGENT.equals(n.properties().get("agent-id")))
                .filter(n -> "scheming".equals(n.properties().get("drive-type")))
                .forEach(n -> store.updateNode(n.id(),
                        io.casehub.neocortex.mindmap.NodeUpdate.empty()
                                .withPropertiesToSet(Map.of("intensity", "0.4")), TENANT));

        var cognition = buildCognition(store, config, true);
        var sections = cognition.renderCognitiveSections(character(), List.of(), Map.of());

        var whatPulls = findSection(sections, "What Pulls at You");
        assertThat(whatPulls).as("'What Pulls at You' must exist").isNotNull();
        var content = ((ObservationSection.TextBlock) whatPulls).content();
        assertThat(content).as("Adapted scheming intensity (0.4) must reach prompt, not original (0.9)")
                .contains("0.4")
                .doesNotContain("0.9");
        assertThat(content).as("Unadapted gallantry must retain original intensity")
                .contains("0.7");
    }

    // ── 14. Relationship overlays (intermediate → e2e) ─────────────────

    @Test
    void relationshipOverlays_evolveAndRenderInPrompt() {
        var store = new InMemoryMindMapStore();
        var targetAgent = "rival-agent";

        // Create people subgraph with entity + overlay
        var peopleSgId = store.createSubgraph(
                new SubgraphInput("people", "social", null), TENANT);

        var targetNodeId = store.addNode(
                NodeInput.of(targetAgent, peopleSgId)
                        .withTraits(Set.of("Entitylike"))
                        .withProperties(Map.of("agentId", targetAgent)), TENANT);

        var overlayProps = new java.util.HashMap<String, String>();
        overlayProps.put(OverlayRef.AGENT_ID, AGENT);
        overlayProps.put("rel.target-id", targetAgent);
        overlayProps.put("rel.role", "rival");
        overlayProps.put("rel.trust", "-0.6");
        overlayProps.put("rel.intimacy", "0.1");
        overlayProps.put("rel.utility", "0.2");
        overlayProps.put("rel.description", "A dangerous adversary who cannot be trusted with any secret");

        store.addNode(
                NodeInput.of(AGENT + "-sees-" + targetAgent, peopleSgId)
                        .withTraits(Set.of("overlay"))
                        .withRefs(Set.of(OverlayRef.of(targetNodeId)))
                        .withProperties(Map.copyOf(overlayProps)), TENANT);

        var schemas = List.of(
                new SocialConfig.RelationalSchema(targetAgent, "rival", -0.6, 0.1, 0.2, "Static config description"));
        var config = new SocialConfig(
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), null,
                null, List.of(), List.of(), schemas, null, List.of(), null, null);

        var cognition = buildCognition(store, config, true);
        var sections = cognition.renderCognitiveSections(
                character(), List.of(targetAgent), Map.of(targetAgent, "The Rival"));

        var feelSection = findSection(sections, "How You Feel About Those Present");
        assertThat(feelSection).as("'How You Feel' section must exist for evolved overlay").isNotNull();
        var content = ((ObservationSection.TextBlock) feelSection).content();
        assertThat(content).as("Evolved overlay description must reach prompt (not static config)")
                .contains("dangerous adversary");
        assertThat(content).as("Display name must be used").contains("The Rival");
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private CharacterState character() {
        return new CharacterState(AGENT, "Test Agent", "drawing-room", 0.0, List.of());
    }

    private CharacterCognition buildCognition(InMemoryMindMapStore store, SocialConfig config, boolean briefingMode) {
        return new CharacterCognition(
                AGENT, null, null, config, List.of(),
                null, new ManorContextStrategy(), null, null,
                TENANT, store, null, null, briefingMode);
    }

    private void seedDriveNode(InMemoryMindMapStore store, String sgId, String type, double intensity) {
        store.addNode(NodeInput.of(type, sgId)
                .withProperties(Map.of(
                        "cognitiveKind", "drive-intensity",
                        "agent-id", AGENT,
                        "drive-type", type,
                        "intensity", String.valueOf(intensity))), TENANT);
    }

    private static ObservationSection findSection(List<ObservationSection> sections, String header) {
        return sections.stream()
                .filter(s -> header.equals(s.header()))
                .findFirst().orElse(null);
    }
}

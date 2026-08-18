package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.ObservationResult;
import io.casehub.blocks.summarisation.observation.ObservationTier;
import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.engine.WorldState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationBuilderTest {

    private WorldState       world;
    private io.casehub.blocks.summarisation.observation.PartitionedDrain<String> emptyDrain;

    @BeforeEach
    void setUp() {
        world      = MansionLoader.loadWorld();
        emptyDrain = new io.casehub.blocks.summarisation.observation.PartitionedDrain<>(ObservationResult.empty(0), java.util.Map.of());
    }

    @Test
    void observation_includes_current_room() {
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("== Current Location ==");
        assertThat(obs).contains("Entrance Hall");
    }

    @Test
    void observation_shows_visible_objects() {
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("Coat Rack");
        assertThat(obs).contains("Guest Book");
        assertThat(obs).doesNotContain("Rat Poison");
    }

    @Test
    void hooded_claw_sees_poison_in_kitchen() {
        world.moveCharacter("hooded-claw", "kitchen");
        var obs = ObservationBuilder.buildObservation(
                world.character("hooded-claw"), world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("Rat Poison");
        assertThat(obs).contains("[TAKE to pick up]");
    }

    @Test
    void penelope_sees_poison_when_in_kitchen() {
        world.moveCharacter("penelope-pitstop", "kitchen");
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("Rat Poison");
    }

    @Test
    void observation_lists_other_characters_in_room() {
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("== Characters Present ==");
        assertThat(obs).contains("The Hooded Claw");
        assertThat(obs).doesNotContain("Penelope Pitstop");
    }

    @Test
    void observation_shows_alone_when_no_others() {
        world.moveCharacter("penelope-pitstop", "kitchen");
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("You are alone.");
    }

    @Test
    void observation_shows_inventory() {
        var dastardly = world.character("dick-dastardly");
        var obs       = ObservationBuilder.buildObservation(dastardly, world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("== Your Inventory ==");
        assertThat(obs).contains("fake-medal");
    }

    @Test
    void observation_shows_empty_inventory() {
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("You are carrying nothing.");
    }

    @Test
    void observation_renders_recent_activity_from_drain() {
        var currentRoom = new ObservationResult("- [1s ago] Penelope: Hello!\n",
                                                java.util.List.of(), 1, 1000, ObservationTier.VERBATIM);
        var drain = new io.casehub.blocks.summarisation.observation.PartitionedDrain<String>(currentRoom, java.util.Map.of());
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), drain);
        assertThat(obs).contains("== Recent Activity ==");
        assertThat(obs).contains("Penelope: Hello!");
        assertThat(obs).doesNotContain("== Remembered ==");
    }

    @Test
    void observation_shows_quiet_room_when_drain_empty() {
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("The room is quiet.");
    }

    @Test
    void observation_renders_remembered_section() {
        var currentRoom = ObservationResult.empty(0);
        var rememberedResult = new ObservationResult("Sneekly examined the cabinet.\n",
                                                     java.util.List.of(), 1, 5000, ObservationTier.GROUPED);
        var remembered = new java.util.LinkedHashMap<String, io.casehub.blocks.summarisation.observation.RememberedPartition>();
        remembered.put("kitchen", new io.casehub.blocks.summarisation.observation.RememberedPartition(rememberedResult, System.currentTimeMillis() - 30000));
        var drain = new io.casehub.blocks.summarisation.observation.PartitionedDrain<>(currentRoom, remembered);
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), drain);
        assertThat(obs).contains("== Remembered ==");
        assertThat(obs).contains("Kitchen");
        assertThat(obs).contains("Sneekly examined the cabinet.");
    }

    @Test
    void observation_shows_interactable_hints() {
        world.moveCharacter("penelope-pitstop", "kitchen");
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("[INTERACT, requires: brass-key]");
    }

    @Test
    void observation_includes_goals_section() {
        var goals = java.util.List.of(
                new io.casehub.eidos.api.AgentGoal("find-diamond", "Find the Doily Diamond",
                                                   io.casehub.eidos.api.GoalPriority.PRIMARY, io.casehub.eidos.api.Visibility.PUBLIC, java.util.List.of()),
                new io.casehub.eidos.api.AgentGoal("solve-puzzles", "Solve puzzles",
                                                   io.casehub.eidos.api.GoalPriority.SECONDARY, io.casehub.eidos.api.Visibility.PUBLIC, java.util.List.of())
                                     );
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, goals, emptyDrain);
        assertThat(obs).contains("== Your Goals ==");
        assertThat(obs).contains("[PRIMARY] Find the Doily Diamond");
        assertThat(obs).contains("[SECONDARY] Solve puzzles");
    }

    @Test
    void observation_includes_last_action_result() {
        var character = world.character("penelope-pitstop");
        character.setLastActionResult("You moved to the Kitchen.");
        var obs = ObservationBuilder.buildObservation(character, world, java.util.List.of(), emptyDrain);
        assertThat(obs).contains("== Last Action Result ==");
        assertThat(obs).contains("You moved to the Kitchen.");
    }

    @Test
    void goals_sorted_by_priority_then_name() {
        var goals = java.util.List.of(
                new io.casehub.eidos.api.AgentGoal("z-secondary", "Z goal",
                                                   io.casehub.eidos.api.GoalPriority.SECONDARY, io.casehub.eidos.api.Visibility.PUBLIC, java.util.List.of()),
                new io.casehub.eidos.api.AgentGoal("a-primary", "A goal",
                                                   io.casehub.eidos.api.GoalPriority.PRIMARY, io.casehub.eidos.api.Visibility.PUBLIC, java.util.List.of())
                                     );
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, goals, emptyDrain);
        int primaryIdx   = obs.indexOf("[PRIMARY]");
        int secondaryIdx = obs.indexOf("[SECONDARY]");
        assertThat(primaryIdx).isLessThan(secondaryIdx);
    }

    @Test
    void pastExperienceSectionRendersMemories() {
        var memories = java.util.List.of(
                new io.casehub.neocortex.memory.Memory("m1", "penelope-pitstop",
                                                       new io.casehub.neocortex.memory.MemoryDomain("manor"), "test",
                                                       null, "Found a key in the library", java.util.Map.of(),
                                                       java.time.Instant.now().minusSeconds(300), 0.8),
                new io.casehub.neocortex.memory.Memory("m2", "penelope-pitstop",
                                                       new io.casehub.neocortex.memory.MemoryDomain("manor"), "test",
                                                       null, "Spoke with Dastardly about the mystery", java.util.Map.of(),
                                                       java.time.Instant.now().minusSeconds(60), 0.6));

        String observation = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain, memories);

        assertThat(observation).contains("Past Experience");
        assertThat(observation).contains("Found a key in the library");
        assertThat(observation).contains("Spoke with Dastardly about the mystery");
    }

    @Test
    void emptyMemoriesOmitsPastExperienceSection() {
        String observation = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(),
                emptyDrain, java.util.List.of());

        assertThat(observation).doesNotContain("Past Experience");
    }


    @Test
    void observation_accepts_capability_tags() {
        var tags = java.util.Set.of("perception", "villain");
        var obs = ObservationBuilder.buildObservation(
                world.character("hooded-claw"), world, java.util.List.of(), emptyDrain,
                java.util.List.of(), tags);
        assertThat(obs).contains("== Current Location ==");
        assertThat(obs).contains("Entrance Hall");
    }

    @Test
    void perceptive_observer_sees_keen_observations() {
        world.addEvent(new io.casehub.examples.manor.model.ManorEvent(
                java.time.Instant.now(), "action", "hooded-claw", "entrance-hall",
                "The Hooded Claw picked up something.",
                io.casehub.examples.manor.model.ActionType.TAKE, "poison", null, null,
                "The Hooded Claw picked up the poison."));
        var tags = java.util.Set.of("perception");
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain,
                java.util.List.of(), tags);
        assertThat(obs).contains("== Keen Observations ==");
        assertThat(obs).contains("The Hooded Claw picked up the poison.");
    }

    @Test
    void non_perceptive_observer_has_no_keen_observations() {
        world.addEvent(new io.casehub.examples.manor.model.ManorEvent(
                java.time.Instant.now(), "action", "hooded-claw", "entrance-hall",
                "The Hooded Claw picked up something.",
                io.casehub.examples.manor.model.ActionType.TAKE, "poison", null, null,
                "The Hooded Claw picked up the poison."));
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain,
                java.util.List.of(), java.util.Set.of());
        assertThat(obs).doesNotContain("Keen Observations");
        assertThat(obs).doesNotContain("picked up the poison");
    }

    @Test
    void observation_includes_current_thinking_when_set() {
        var character = world.character("hooded-claw");
        character.setCurrentThinking("I see the poison on the shelf — I should take it now.");
        var obs = ObservationBuilder.buildObservation(character, world, java.util.List.of(), emptyDrain, java.util.List.of(), character.capabilityTags());
        assertThat(obs).contains("== Your Current Thinking ==");
        assertThat(obs).contains("I see the poison on the shelf");
    }

    @Test
    void observation_omits_thinking_section_when_null() {
        var character = world.character("hooded-claw");
        var obs       = ObservationBuilder.buildObservation(character, world, java.util.List.of(), emptyDrain, java.util.List.of(), character.capabilityTags());
        assertThat(obs).doesNotContain("Your Current Thinking");
    }

    @Test
    void exchange_observation_includes_room_and_dialogue_only() {
        var obs = ObservationBuilder.buildExchangeObservation(
                world.character("hooded-claw"), "What do you need, boss?", world);
        assertThat(obs).contains("Entrance Hall");
        assertThat(obs).contains("What do you need, boss?");
        assertThat(obs).doesNotContain("== Visible Objects ==");
        assertThat(obs).doesNotContain("== Exits ==");
    }

    @Test
    void exchange_observation_includes_current_thinking() {
        var character = world.character("hooded-claw");
        character.setCurrentThinking("Get Muttley to fetch the key.");
        var obs = ObservationBuilder.buildExchangeObservation(character, "Hehehehe!", world);
        assertThat(obs).contains("Your Current Thinking");
        assertThat(obs).contains("Get Muttley to fetch the key.");
    }

    @Test
    void plan_sections_render_per_goal_with_status() {
        var character = world.character("hooded-claw");
        var step1     = new io.casehub.examples.manor.model.PlanStep("s1", "Find the poison", io.casehub.examples.manor.model.PlanStepStatus.COMPLETED);
        var step2     = new io.casehub.examples.manor.model.PlanStep("s2", "Dispose of it", io.casehub.examples.manor.model.PlanStepStatus.PENDING);
        var plan      = new io.casehub.examples.manor.model.AgentPlan("protect-penelope", java.util.List.of(step1, step2), "protect her", 1, 1, 0);
        character.setPlan("protect-penelope", plan);
        var obs = ObservationBuilder.buildObservation(character, world, java.util.List.of(), emptyDrain, java.util.List.of(), character.capabilityTags());
        assertThat(obs).contains("Plan: protect-penelope");
        assertThat(obs).contains("[COMPLETED] Find the poison");
        assertThat(obs).contains("[PENDING] Dispose of it");
    }

    @Test
    void plan_sections_empty_when_no_plans() {
        var character = world.character("hooded-claw");
        var obs       = ObservationBuilder.buildObservation(character, world, java.util.List.of(), emptyDrain, java.util.List.of(), character.capabilityTags());
        assertThat(obs).doesNotContain("Plan:");
    }

    @Test
    void insightsSectionRendersWhenReflectionsProvided() {
        var reflections = java.util.List.of(
                new io.casehub.neocortex.memory.Memory("r1", "penelope-pitstop",
                                                       io.casehub.neocortex.memory.reflection.ReflectionEvents.DOMAIN, "t1",
                                                       null, "Sneekly is always near dangerous items",
                                                       java.util.Map.of(), java.time.Instant.now(), 0.8)
                                           );

        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain,
                java.util.List.of(), reflections, java.util.Map.of(), java.util.Set.of());

        assertThat(obs).contains("Insights");
        assertThat(obs).contains("Sneekly is always near dangerous items");
    }

    @Test
    void insightsSectionOmittedWhenNoReflections() {
        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain,
                java.util.List.of(), java.util.List.of(), java.util.Map.of(), java.util.Set.of());

        assertThat(obs).doesNotContain("Insights");
    }

    @Test
    void relationshipNotesRenderForCharactersInRoom() {
        var relationships = java.util.Map.of(
                "hooded-claw", java.util.List.of(
                        new io.casehub.neocortex.memory.Memory("rel1", "penelope-pitstop",
                                                               new io.casehub.neocortex.memory.MemoryDomain("relationship"), "t1",
                                                               null, "Sneekly offered you tea with unusual insistence",
                                                               java.util.Map.of(), java.time.Instant.now(), 0.7)
                                                )
                                            );

        var obs = ObservationBuilder.buildObservation(
                world.character("penelope-pitstop"), world, java.util.List.of(), emptyDrain,
                java.util.List.of(), java.util.List.of(), relationships, java.util.Set.of());

        assertThat(obs).contains("About The Hooded Claw");
        assertThat(obs).contains("Sneekly offered you tea with unusual insistence");
    }

}

package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorldStateTest {

    private WorldState world;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
    }

    @Test
    void loads_three_rooms() {
        assertThat(world.rooms()).hasSize(3);
        assertThat(world.rooms()).containsKeys("entrance-hall", "kitchen", "ballroom");
    }

    @Test
    void rooms_have_correct_adjacency() {
        Room kitchen = world.room("kitchen");
        assertThat(kitchen.adjacentRooms()).containsExactlyInAnyOrder("entrance-hall", "ballroom");
    }

    @Test
    void loads_five_characters() {
        assertThat(world.characters()).hasSize(5);
        assertThat(world.characters()).containsKeys(
            "penelope", "hooded-claw", "ant-hill-mob", "dick-dastardly", "peter-perfect");
    }

    @Test
    void characters_start_in_entrance_hall() {
        for (CharacterState c : world.characters().values()) {
            assertThat(c.currentRoom()).isEqualTo("entrance-hall");
        }
    }

    @Test
    void dastardly_starts_with_fake_medal() {
        CharacterState dastardly = world.character("dick-dastardly");
        assertThat(dastardly.inventory()).containsExactly("fake-medal");
    }

    @Test
    void poison_visible_only_to_hooded_claw() {
        var hcVisible = world.visibleObjects("kitchen", "hooded-claw");
        assertThat(hcVisible).anyMatch(o -> o.id().equals("poison"));

        var penelopeVisible = world.visibleObjects("kitchen", "penelope");
        assertThat(penelopeVisible).noneMatch(o -> o.id().equals("poison"));
    }

    @Test
    void reveal_object_makes_it_visible_to_character() {
        world.revealObject("poison", "penelope");
        var penelopeVisible = world.visibleObjects("kitchen", "penelope");
        assertThat(penelopeVisible).anyMatch(o -> o.id().equals("poison"));
    }

    @Test
    void move_character_updates_room() {
        world.moveCharacter("penelope", "kitchen");
        assertThat(world.character("penelope").currentRoom()).isEqualTo("kitchen");
    }

    @Test
    void add_and_remove_inventory() {
        world.addToInventory("penelope", "brass-key");
        assertThat(world.character("penelope").inventory()).contains("brass-key");

        world.removeFromInventory("penelope", "brass-key");
        assertThat(world.character("penelope").inventory()).doesNotContain("brass-key");
    }

    @Test
    void mark_character_inactive() {
        world.markCharacterInactive("peter-perfect");
        assertThat(world.character("peter-perfect").isActive()).isFalse();
    }

    @Test
    void scenario_complete_lifecycle() {
        assertThat(world.isScenarioComplete()).isFalse();
        world.setScenarioComplete();
        assertThat(world.isScenarioComplete()).isTrue();
    }

    @Test
    void recent_events_returns_most_recent_first() {
        world.addEvent("dialogue", "penelope", "entrance-hall", "Why, how delightful!");
        world.addEvent("action", "hooded-claw", "entrance-hall", "moved to kitchen");
        world.addEvent("dialogue", "dastardly", "entrance-hall", "Mehehehe!");

        var events = world.recentEvents("entrance-hall", 2);
        assertThat(events).hasSize(2);
        assertThat(events.get(0).description()).isEqualTo("Mehehehe!");
        assertThat(events.get(1).description()).isEqualTo("moved to kitchen");
    }

    @Test
    void objects_with_no_visibleTo_are_visible_to_everyone() {
        var objects = world.visibleObjects("entrance-hall", "penelope");
        assertThat(objects).anyMatch(o -> o.id().equals("coat-rack"));
        assertThat(objects).anyMatch(o -> o.id().equals("guest-book"));
    }

    @Test
    void characters_in_room_excludes_inactive() {
        assertThat(world.charactersInRoom("entrance-hall")).hasSize(5);
        world.markCharacterInactive("peter-perfect");
        assertThat(world.charactersInRoom("entrance-hall")).hasSize(4);
    }

    @Test
    void find_object_across_rooms() {
        assertThat(world.findObject("poison")).isNotNull();
        assertThat(world.findObject("poison").name()).isEqualTo("Rat Poison");
        assertThat(world.findObjectRoom("poison")).isEqualTo("kitchen");
    }

    @Test
    void scene_completed_lifecycle() {
        assertThat(world.isSceneCompleted("tea-poisoning")).isFalse();
        world.markSceneCompleted("tea-poisoning");
        assertThat(world.isSceneCompleted("tea-poisoning")).isTrue();
    }
}

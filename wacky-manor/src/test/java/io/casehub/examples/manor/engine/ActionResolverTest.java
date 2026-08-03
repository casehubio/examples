package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActionResolverTest {

    private WorldState world;
    private ActionResolver resolver;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        resolver = new ActionResolver();
    }

    @Test
    void move_to_adjacent_room_succeeds() {
        var penelope = world.character("penelope-pitstop");
        var result = resolver.resolve(penelope,
            new Action(ActionType.MOVE, "kitchen", null), world);
        assertThat(result).isInstanceOf(ActionResult.MovedToRoom.class);
        assertThat(penelope.currentRoom()).isEqualTo("kitchen");
    }

    @Test
    void move_to_non_adjacent_room_fails() {
        var penelope = world.character("penelope-pitstop");
        var result = resolver.resolve(penelope,
            new Action(ActionType.MOVE, "ballroom", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
        assertThat(penelope.currentRoom()).isEqualTo("entrance-hall");
    }

    @Test
    void take_portable_object_adds_to_inventory() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "kitchen");

        var result = resolver.resolve(hc,
                                      new Action(ActionType.TAKE, "poison", null), world);
        assertThat(result).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(hc.hasItem("rat-poison")).isTrue();
    }

    @Test
    void take_non_portable_object_fails() {
        var penelope = world.character("penelope-pitstop");
        penelope.setX(0.5);
        var result = resolver.resolve(penelope,
            new Action(ActionType.TAKE, "guest-book", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void interact_without_required_item_fails() {
        var penelope = world.character("penelope-pitstop");
        world.moveCharacter("penelope-pitstop", "kitchen");
        penelope.setX(0.3);
        var result = resolver.resolve(penelope,
                                      new Action(ActionType.INTERACT, "cabinet", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void interact_with_required_item_succeeds_and_yields() {
        var dastardly = world.character("dick-dastardly");
        world.moveCharacter("dick-dastardly", "kitchen");
        dastardly.setX(0.3);
        dastardly.addItem("brass-key");
        var result = resolver.resolve(dastardly,
                                      new Action(ActionType.INTERACT, "cabinet", "brass-key"), world);
        assertThat(result).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(dastardly.hasItem("old-recipe-cards")).isTrue();
        assertThat(dastardly.hasItem("brass-key")).isFalse();
    }

    @Test
    void give_transfers_item_to_character_in_same_room() {
        var dastardly = world.character("dick-dastardly");
        var penelope = world.character("penelope-pitstop");
        dastardly.setX(0.3);
        penelope.setX(0.3);

        var result = resolver.resolve(dastardly,
            new Action(ActionType.GIVE, "penelope-pitstop", "fake-medal"), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
        assertThat(dastardly.hasItem("fake-medal")).isFalse();
        assertThat(penelope.hasItem("fake-medal")).isTrue();
    }

    @Test
    void give_to_character_in_different_room_fails() {
        var dastardly = world.character("dick-dastardly");
        world.moveCharacter("penelope-pitstop", "kitchen");

        var result = resolver.resolve(dastardly,
            new Action(ActionType.GIVE, "penelope-pitstop", "fake-medal"), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void use_item_on_compatible_object() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "ballroom");
        hc.setX(0.5);
        hc.addItem("rat-poison");

        var result = resolver.resolve(hc,
            new Action(ActionType.USE, "tea-service", "rat-poison"), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
    }

    @Test
    void use_incompatible_item_on_object_fails() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "ballroom");
        hc.setX(0.5);
        hc.addItem("brass-key");

        var result = resolver.resolve(hc,
            new Action(ActionType.USE, "tea-service", "brass-key"), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void look_always_succeeds() {
        var penelope = world.character("penelope-pitstop");
        var result = resolver.resolve(penelope,
            new Action(ActionType.LOOK, "coat-rack", null), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
    }

    @Test
    void wait_always_succeeds() {
        var penelope = world.character("penelope-pitstop");
        var result = resolver.resolve(penelope,
            new Action(ActionType.WAIT, null, null), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
    }

    @Test
    void interact_auto_positions_to_distant_object() {
        var penelope = world.character("penelope-pitstop");
        world.moveCharacter("penelope-pitstop", "kitchen");
        penelope.setX(0.1);
        penelope.addItem("brass-key");

        var result = resolver.resolve(penelope,
                                      new Action(ActionType.INTERACT, "cabinet", "brass-key"), world);
        assertThat(result).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(penelope.x()).isEqualTo(0.3);
    }

    @Test
    void look_at_room_without_target() {
        var penelope = world.character("penelope-pitstop");
        var result = resolver.resolve(penelope,
            new Action(ActionType.LOOK, null, null), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
        assertThat(((ActionResult.Success) result).description()).contains("Entrance Hall");
    }

    @Test
    void give_item_not_in_inventory_fails() {
        var penelope = world.character("penelope-pitstop");
        var result = resolver.resolve(penelope,
            new Action(ActionType.GIVE, "dick-dastardly", "nonexistent"), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void action_result_text_returns_description_for_all_variants() {
        assertThat(new ActionResult.Success("ok").text()).isEqualTo("ok");
        assertThat(new ActionResult.Failed("nope").text()).isEqualTo("nope");
        assertThat(new ActionResult.MovedToRoom("kitchen", "You moved.").text()).isEqualTo("You moved.");
        assertThat(new ActionResult.ItemReceived("key", "Got key.").text()).isEqualTo("Got key.");
    }

    @Test
    void take_auto_positions_character_to_object() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "kitchen");
        assertThat(hc.x()).isEqualTo(0.5);

        resolver.resolve(hc, new Action(ActionType.TAKE, "poison", null), world);
        assertThat(hc.x()).isEqualTo(0.7);
    }

    @Test
    void interact_auto_positions_character_to_object() {
        var dastardly = world.character("dick-dastardly");
        world.moveCharacter("dick-dastardly", "kitchen");
        dastardly.addItem("brass-key");
        assertThat(dastardly.x()).isNotEqualTo(0.3);

        resolver.resolve(dastardly, new Action(ActionType.INTERACT, "cabinet", "brass-key"), world);
        assertThat(dastardly.x()).isEqualTo(0.3);
    }

    @Test
    void take_marks_object_as_taken() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "kitchen");

        resolver.resolve(hc, new Action(ActionType.TAKE, "poison", null), world);
        assertThat(world.isObjectTaken("poison")).isTrue();
        assertThat(hc.hasItem("rat-poison")).isTrue();
    }

    @Test
    void take_same_object_twice_fails() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "kitchen");

        resolver.resolve(hc, new Action(ActionType.TAKE, "poison", null), world);
        var secondTake = resolver.resolve(hc, new Action(ActionType.TAKE, "poison", null), world);
        assertThat(secondTake).isInstanceOf(ActionResult.Failed.class);
        assertThat(secondTake.text()).contains("no longer here");
    }

    @Test
    void use_consumes_item_from_inventory() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "ballroom");
        hc.addItem("rat-poison");

        resolver.resolve(hc, new Action(ActionType.USE, "tea-service", "rat-poison"), world);
        assertThat(hc.hasItem("rat-poison")).isFalse();
    }

    @Test
    void use_applies_effect_to_world_state() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "ballroom");
        hc.addItem("rat-poison");

        resolver.resolve(hc, new Action(ActionType.USE, "tea-service", "rat-poison"), world);
        assertThat(world.hasEffect("tea-service", "rat-poison")).isTrue();
    }

    @Test
    void actions_on_taken_objects_fail() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "kitchen");
        world.markObjectTaken("stove");

        var result = resolver.resolve(hc, new Action(ActionType.LOOK, "stove", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }
}

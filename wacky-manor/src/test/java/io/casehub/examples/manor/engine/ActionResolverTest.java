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
        hc.setX(0.7);

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
        penelope.setX(0.8);
        var result = resolver.resolve(penelope,
            new Action(ActionType.INTERACT, "muttley", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void interact_with_required_item_succeeds_and_yields() {
        var dastardly = world.character("dick-dastardly");
        dastardly.setX(0.8);
        var result = resolver.resolve(dastardly,
            new Action(ActionType.INTERACT, "muttley", "fake-medal"), world);
        assertThat(result).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(dastardly.hasItem("brass-key")).isTrue();
        assertThat(dastardly.hasItem("fake-medal")).isFalse();
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
    void proximity_check_rejects_distant_interaction() {
        var penelope = world.character("penelope-pitstop");
        world.moveCharacter("penelope-pitstop", "kitchen");
        penelope.setX(0.1);

        var result = resolver.resolve(penelope,
            new Action(ActionType.INTERACT, "cabinet", "brass-key"), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
        assertThat(((ActionResult.Failed) result).reason()).contains("too far");
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
}

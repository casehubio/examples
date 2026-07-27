package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActionResolverEdgeCaseTest {

    private WorldState world;
    private ActionResolver resolver;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        resolver = new ActionResolver();
    }

    // -- Proximity boundary tests --

    @Test
    void interact_at_exact_proximity_threshold_succeeds() {
        var penelope = world.character("penelope-pitstop");
        penelope.setX(0.65);
        var result = resolver.resolve(penelope,
            new Action(ActionType.LOOK, "guest-book", null), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
    }

    @Test
    void interact_auto_positions_from_far_away() {
        var penelope = world.character("penelope-pitstop");
        penelope.setX(0.0);
        penelope.addItem("fake-medal");
        var result = resolver.resolve(penelope,
                                      new Action(ActionType.INTERACT, "muttley", "fake-medal"), world);
        assertThat(result).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(penelope.x()).isEqualTo(0.8);
    }

    @Test
    void character_at_position_zero() {
        var penelope = world.character("penelope-pitstop");
        penelope.setX(0.0);
        var result = resolver.resolve(penelope,
            new Action(ActionType.LOOK, null, null), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
    }

    @Test
    void character_at_position_one() {
        var penelope = world.character("penelope-pitstop");
        penelope.setX(1.0);
        var result = resolver.resolve(penelope,
            new Action(ActionType.WAIT, null, null), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
    }

    // -- Error paths --

    @Test
    void interact_with_nonexistent_object_fails() {
        var penelope = world.character("penelope-pitstop");
        var result = resolver.resolve(penelope,
            new Action(ActionType.INTERACT, "nonexistent", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
        assertThat(((ActionResult.Failed) result).reason()).contains("No such object");
    }

    @Test
    void move_to_unknown_room_fails() {
        var penelope = world.character("penelope-pitstop");
        var result = resolver.resolve(penelope,
            new Action(ActionType.MOVE, "dungeon", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void give_without_specifying_item_fails() {
        var penelope = world.character("penelope-pitstop");
        var result = resolver.resolve(penelope,
            new Action(ActionType.GIVE, "dick-dastardly", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
    }

    @Test
    void use_item_not_in_inventory_fails() {
        var penelope = world.character("penelope-pitstop");
        world.moveCharacter("penelope-pitstop", "ballroom");
        penelope.setX(0.5);
        var result = resolver.resolve(penelope,
            new Action(ActionType.USE, "tea-service", "rat-poison"), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
        assertThat(((ActionResult.Failed) result).reason()).contains("don't have");
    }

    @Test
    void take_non_portable_object_gives_clear_message() {
        var penelope = world.character("penelope-pitstop");
        penelope.setX(0.2);
        var result = resolver.resolve(penelope,
            new Action(ActionType.TAKE, "coat-rack", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
        assertThat(((ActionResult.Failed) result).reason()).contains("cannot be picked up");
    }

    @Test
    void interact_with_non_interactable_object_gives_clear_message() {
        var penelope = world.character("penelope-pitstop");
        penelope.setX(0.2);
        var result = resolver.resolve(penelope,
            new Action(ActionType.INTERACT, "coat-rack", null), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
        assertThat(((ActionResult.Failed) result).reason()).contains("cannot be interacted");
    }

    @Test
    void give_auto_positions_to_distant_character() {
        var dastardly = world.character("dick-dastardly");
        var penelope  = world.character("penelope-pitstop");
        dastardly.setX(0.0);
        penelope.setX(1.0);
        var result = resolver.resolve(dastardly,
                                      new Action(ActionType.GIVE, "penelope-pitstop", "fake-medal"), world);
        assertThat(result).isInstanceOf(ActionResult.Success.class);
        assertThat(dastardly.x()).isEqualTo(1.0);
    }

    @Test
    void interact_with_required_item_but_wrong_item() {
        var dastardly = world.character("dick-dastardly");
        dastardly.setX(0.8);
        var result = resolver.resolve(dastardly,
            new Action(ActionType.INTERACT, "muttley", "brass-key"), world);
        assertThat(result).isInstanceOf(ActionResult.Failed.class);
        assertThat(((ActionResult.Failed) result).reason()).contains("requires");
    }

    // -- State edge cases --

    @Test
    void move_updates_position_to_center() {
        var penelope = world.character("penelope-pitstop");
        penelope.setX(0.1);
        resolver.resolve(penelope,
            new Action(ActionType.MOVE, "kitchen", null), world);
        assertThat(penelope.x()).isEqualTo(0.5);
    }

    @Test
    void give_removes_item_from_giver_and_adds_to_receiver() {
        var dastardly = world.character("dick-dastardly");
        var penelope = world.character("penelope-pitstop");
        dastardly.setX(0.3);
        penelope.setX(0.3);

        assertThat(dastardly.hasItem("fake-medal")).isTrue();
        assertThat(penelope.hasItem("fake-medal")).isFalse();

        resolver.resolve(dastardly,
            new Action(ActionType.GIVE, "penelope-pitstop", "fake-medal"), world);

        assertThat(dastardly.hasItem("fake-medal")).isFalse();
        assertThat(penelope.hasItem("fake-medal")).isTrue();
    }
}

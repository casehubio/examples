package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemDependencyTest {

    private WorldState world;
    private ActionResolver resolver;

    @BeforeEach
    void setUp() {
        world = MansionLoader.loadWorld();
        resolver = new ActionResolver();
    }

    @Test
    void full_item_chain_fake_medal_to_recipe_cards() {
        var dastardly = world.character("dick-dastardly");
        dastardly.addItem("brass-key");

        world.moveCharacter("dick-dastardly", "kitchen");
        dastardly.setX(0.3);

        var result = resolver.resolve(dastardly,
                                      new Action(ActionType.INTERACT, "cabinet", "brass-key"), world);
        assertThat(result).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(dastardly.hasItem("old-recipe-cards")).isTrue();
    }

    @Test
    void poison_chain_take_then_use_on_tea() {
        var hc = world.character("hooded-claw");
        world.moveCharacter("hooded-claw", "kitchen");
        hc.setX(0.7);

        var take = resolver.resolve(hc,
            new Action(ActionType.TAKE, "poison", null), world);
        assertThat(take).isInstanceOf(ActionResult.ItemReceived.class);
        assertThat(hc.hasItem("rat-poison")).isTrue();

        world.moveCharacter("hooded-claw", "ballroom");
        hc.setX(0.5);

        var use = resolver.resolve(hc,
            new Action(ActionType.USE, "tea-service", "rat-poison"), world);
        assertThat(use).isInstanceOf(ActionResult.Success.class);
    }
}

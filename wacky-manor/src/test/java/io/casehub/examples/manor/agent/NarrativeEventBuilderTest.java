package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NarrativeEventBuilderTest {

    private final CharacterState hc = new CharacterState(
        "hooded-claw", "The Hooded Claw (as Sneekly)", "kitchen", 0.5, List.of());

    @Test
    void move_produces_narrative() {
        var result = NarrativeEventBuilder.describe(hc,
            new Action(ActionType.MOVE, "ballroom", null),
            new ActionResult.MovedToRoom("ballroom", "You moved to Ballroom."));
        assertThat(result).isEqualTo("The Hooded Claw (as Sneekly) walked into the ballroom.");
    }

    @Test
    void take_produces_vague_narrative() {
        var result = NarrativeEventBuilder.describe(hc,
            new Action(ActionType.TAKE, "poison", null),
            new ActionResult.ItemReceived("rat-poison", "You picked up Rat Poison."));
        assertThat(result).isEqualTo("The Hooded Claw (as Sneekly) picked up something.");
    }

    @Test
    void use_produces_vague_narrative() {
        var result = NarrativeEventBuilder.describe(hc,
            new Action(ActionType.USE, "tea-service", "rat-poison"),
            new ActionResult.Success("You used rat-poison on Tea Service."));
        assertThat(result).isEqualTo("The Hooded Claw (as Sneekly) fussed with the tea-service for a moment.");
    }

    @Test
    void give_produces_narrative() {
        var result = NarrativeEventBuilder.describe(hc,
            new Action(ActionType.GIVE, "penelope-pitstop", "fake-medal"),
            new ActionResult.Success("You gave fake-medal to Penelope Pitstop."));
        assertThat(result).isEqualTo("The Hooded Claw (as Sneekly) handed something to penelope-pitstop.");
    }

    @Test
    void look_produces_narrative() {
        var result = NarrativeEventBuilder.describe(hc,
            new Action(ActionType.LOOK, "cabinet", null),
            new ActionResult.Success("You examine Locked Cabinet."));
        assertThat(result).isEqualTo("The Hooded Claw (as Sneekly) examined the cabinet.");
    }

    @Test
    void interact_produces_narrative() {
        var result = NarrativeEventBuilder.describe(hc,
                                                    new Action(ActionType.INTERACT, "cabinet", "brass-key"),
                                                    new ActionResult.ItemReceived("old-recipe-cards", "You received old-recipe-cards."));
        assertThat(result).isEqualTo("The Hooded Claw (as Sneekly) interacted with the cabinet.");
    }

    @Test
    void wait_returns_null() {
        var result = NarrativeEventBuilder.describe(hc,
            new Action(ActionType.WAIT, null, null),
            new ActionResult.Success("You wait and observe."));
        assertThat(result).isNull();
    }

    @Test
    void failed_action_returns_null() {
        var result = NarrativeEventBuilder.describe(hc,
            new Action(ActionType.TAKE, "cabinet", null),
            new ActionResult.Failed("Not portable."));
        assertThat(result).isNull();
    }

    @Test
    void take_rich_reveals_item_in_detailed() {
        var result = NarrativeEventBuilder.describeRich(hc, new Action(ActionType.TAKE, "poison", null), new ActionResult.ItemReceived("rat-poison", "You picked up Rat Poison."));
        assertThat(result).isNotNull();
        assertThat(result.publicText()).isEqualTo("The Hooded Claw (as Sneekly) picked up something.");
        assertThat(result.detailedText()).contains("poison");
    }

    @Test
    void use_rich_reveals_applied_item() {
        var result = NarrativeEventBuilder.describeRich(hc, new Action(ActionType.USE, "tea-service", "rat-poison"), new ActionResult.Success("ok"));
        assertThat(result).isNotNull();
        assertThat(result.publicText()).contains("fussed with");
        assertThat(result.detailedText()).contains("rat-poison");
    }

    @Test
    void move_rich_has_null_detailed() {
        var result = NarrativeEventBuilder.describeRich(hc, new Action(ActionType.MOVE, "ballroom", null), new ActionResult.MovedToRoom("ballroom", "ok"));
        assertThat(result).isNotNull();
        assertThat(result.detailedText()).isNull();
    }

    @Test
    void wait_rich_returns_null() {
        var result = NarrativeEventBuilder.describeRich(hc, new Action(ActionType.WAIT, null, null), new ActionResult.Success("ok"));
        assertThat(result).isNull();
    }

    @Test
    void failed_rich_returns_null() {
        var result = NarrativeEventBuilder.describeRich(hc, new Action(ActionType.TAKE, "x", null), new ActionResult.Failed("nope"));
        assertThat(result).isNull();
    }

    @Test
    void steal_public_is_vague() {
        var result = NarrativeEventBuilder.describe(hc, new Action(ActionType.STEAL, "muttley", "brass-key"), new ActionResult.ItemReceived("brass-key", "ok"));
        assertThat(result).contains("did something near");
    }

    @Test
    void steal_rich_reveals_theft() {
        var result = NarrativeEventBuilder.describeRich(hc, new Action(ActionType.STEAL, "muttley", "brass-key"), new ActionResult.ItemReceived("brass-key", "ok"));
        assertThat(result).isNotNull();
        assertThat(result.publicText()).contains("did something near");
        assertThat(result.detailedText()).contains("brass-key");
        assertThat(result.detailedText()).contains("muttley");
    }

    @Test
    void directed_dialogue_public_is_vague() {
        var desc = NarrativeEventBuilder.describeDirectedDialogue("Sneekly", "peter-perfect", "The cellar is safe.");
        assertThat(desc.publicText()).contains("spoke quietly with");
        assertThat(desc.publicText()).contains("peter-perfect");
    }

    @Test
    void directed_dialogue_detailed_includes_content() {
        var desc = NarrativeEventBuilder.describeDirectedDialogue("Sneekly", "peter-perfect", "The cellar is safe.");
        assertThat(desc.detailedText()).contains("cellar is safe");
    }
}

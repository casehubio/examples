package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.ActionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentResponseTest {

    @Test
    void parses_valid_json() {
        String json = """
            {
              "thinking": "I should explore the kitchen.",
              "dialogue": "Why, this kitchen is simply darlin'!",
              "aside": null,
              "action": {
                "type": "MOVE",
                "target": "kitchen",
                "withItem": null
              }
            }
            """;
        AgentResponse response = AgentResponse.parse(json);
        assertThat(response.thinking()).isEqualTo("I should explore the kitchen.");
        assertThat(response.dialogue()).isEqualTo("Why, this kitchen is simply darlin'!");
        assertThat(response.aside()).isNull();
        assertThat(response.action().type()).isEqualTo(ActionType.MOVE);
        assertThat(response.action().target()).isEqualTo("kitchen");
    }

    @Test
    void parses_interact_with_item() {
        String json = """
            {
              "thinking": "I have the key.",
              "dialogue": null,
              "aside": "Nyah-ha-ha!",
              "action": {
                "type": "INTERACT",
                "target": "cabinet",
                "withItem": "brass-key"
              }
            }
            """;
        AgentResponse response = AgentResponse.parse(json);
        assertThat(response.aside()).isEqualTo("Nyah-ha-ha!");
        assertThat(response.action().type()).isEqualTo(ActionType.INTERACT);
        assertThat(response.action().withItem()).isEqualTo("brass-key");
    }

    @Test
    void handles_malformed_json_gracefully() {
        AgentResponse response = AgentResponse.parse("not json at all");
        assertThat(response.action().type()).isEqualTo(ActionType.WAIT);
    }

    @Test
    void extracts_json_from_markdown_code_block() {
        String wrapped = """
            Here is my response:
            ```json
            {"thinking":"test","dialogue":null,"aside":null,"action":{"type":"WAIT","target":null,"withItem":null}}
            ```
            """;
        AgentResponse response = AgentResponse.parse(wrapped);
        assertThat(response.action().type()).isEqualTo(ActionType.WAIT);
    }

    @Test
    void extracts_json_embedded_in_prose() {
        String text = """
            Let me think about this...
            {"thinking":"hmm","dialogue":"Hello!","aside":null,"action":{"type":"LOOK","target":null,"withItem":null}}
            That was my response.
            """;
        AgentResponse response = AgentResponse.parse(text);
        assertThat(response.dialogue()).isEqualTo("Hello!");
        assertThat(response.action().type()).isEqualTo(ActionType.LOOK);
    }

    @Test
    void idle_returns_wait_action() {
        AgentResponse response = AgentResponse.idle();
        assertThat(response.action().type()).isEqualTo(ActionType.WAIT);
        assertThat(response.thinking()).isNull();
        assertThat(response.dialogue()).isNull();
        assertThat(response.aside()).isNull();
    }

    @Test
    void parse_includes_talkTo_field() {
        var json = """
                   {"thinking":"plan","dialogue":"hello","talkTo":"peter-perfect","aside":null,"action":{"type":"WAIT"}}""";
        var response = AgentResponse.parse(json);
        assertThat(response.talkTo()).isEqualTo("peter-perfect");
    }

    @Test
    void parse_talkTo_null_when_absent() {
        var json = """
                   {"thinking":"plan","dialogue":"hello","action":{"type":"WAIT"}}""";
        var response = AgentResponse.parse(json);
        assertThat(response.talkTo()).isNull();
    }

    @Test
    void parse_includes_newGoals() {
        var json = """
                   {"thinking":"t","action":{"type":"WAIT"},"newGoals":[{"name":"protect-tea","description":"Stop the poison"}]}""";
        var response = AgentResponse.parse(json);
        assertThat(response.newGoals()).hasSize(1);
        assertThat(response.newGoals().get(0).name()).isEqualTo("protect-tea");
    }

    @Test
    void parse_includes_dropGoals() {
        var json = """
                   {"thinking":"t","action":{"type":"WAIT"},"dropGoals":["old-goal"]}""";
        var response = AgentResponse.parse(json);
        assertThat(response.dropGoals()).containsExactly("old-goal");
    }

    @Test
    void idle_has_null_for_new_fields() {
        var response = AgentResponse.idle();
        assertThat(response.talkTo()).isNull();
        assertThat(response.newGoals()).isNull();
        assertThat(response.dropGoals()).isNull();
    }
}

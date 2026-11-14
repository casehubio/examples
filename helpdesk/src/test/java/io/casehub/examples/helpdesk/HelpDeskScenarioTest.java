package io.casehub.examples.helpdesk;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DemoTestProfile.class)
class HelpDeskScenarioTest {

    @Test
    void chatMessageCreatesClassifiedTicket() {
        given()
            .contentType("application/json")
            .body(Map.of("ticketClassifications", List.of(
                    Map.of("match", "laptop won't boot",
                            "category", "HARDWARE",
                            "priority", "HIGH"))))
            .when().post("/scenario/bootstrap/helpdesk")
            .then().statusCode(200);

        given()
            .contentType("application/json")
            .body(Map.of("from", "Alice Customer",
                    "channelId", "support",
                    "text", "My laptop won't boot after the update last night"))
            .when().post("/scenario/inject/chat")
            .then().statusCode(202);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var tickets = given()
                    .when().get("/scenario/verify/tickets")
                    .then().statusCode(200)
                    .extract().jsonPath().getList(".", Map.class);
            assertThat(tickets).anyMatch(t -> "TRIAGED".equals(t.get("status")));
            assertThat(tickets).anySatisfy(t -> {
                assertThat(t.get("category")).isEqualTo("HARDWARE");
                assertThat(t.get("customerRef")).isEqualTo("Alice Customer");
            });
        });

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var workItems = given()
                    .when().get("/api/work/items/list-all")
                    .then().statusCode(200)
                    .extract().jsonPath().getList("items");
            assertThat(workItems).isNotEmpty();
        });
    }

    @Test
    void resolveTicketSendsNotification() throws JsonProcessingException {
        given()
            .contentType("application/json")
            .body(Map.of("ticketClassifications", List.of(
                    Map.of("match", "printer", "category", "HARDWARE", "priority", "MEDIUM"))))
            .when().post("/scenario/bootstrap/helpdesk")
            .then().statusCode(200);

        given()
            .contentType("application/json")
            .body(Map.of("from", "Bob User",
                    "channelId", "support",
                    "text", "The printer is jammed"))
            .when().post("/scenario/inject/chat")
            .then().statusCode(202);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var workItems = given()
                    .when().get("/api/work/items/list-all")
                    .then().statusCode(200)
                    .extract().jsonPath()
                    .getList("items.findAll { it.title.contains('printer') }", Map.class);
            assertThat(workItems).isNotEmpty();
        });

        String workItemId = given()
                .when().get("/api/work/items/list-all")
                .then().statusCode(200)
                .extract().jsonPath()
                .getString("items.find { it.title.contains('printer') }.id");

        given().queryParam("claimant", "hw-specialist")
                .when().post("/api/work/lifecycle/claim/" + workItemId)
                .then().statusCode(200);

        given().queryParam("actor", "hw-specialist").when().post("/api/work/lifecycle/start/" + workItemId)
                .then().statusCode(200);

        String resolutionJson = new ObjectMapper().writeValueAsString(Map.of(
                "resolution", "Cleared the paper jam",
                "assigneeId", "hw-specialist"));
        given().queryParam("actor", "hw-specialist").contentType("application/json")
                .body(Map.of("resolution", resolutionJson))
                .when().post("/api/work/lifecycle/complete/" + workItemId)
                .then().statusCode(200);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var notifications = given()
                    .when().get("/scenario/verify/notifications")
                    .then().statusCode(200)
                    .extract().jsonPath().getList(".", Map.class);
            assertThat(notifications).anySatisfy(n -> {
                assertThat(n.get("to")).isEqualTo("Bob User");
                assertThat((String) n.get("message")).contains("Cleared the paper jam");
            });
        });
    }
}

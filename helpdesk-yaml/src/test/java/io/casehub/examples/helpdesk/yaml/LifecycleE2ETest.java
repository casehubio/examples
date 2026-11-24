package io.casehub.examples.helpdesk.yaml;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import io.casehub.api.model.CaseStatus;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.work.runtime.service.WorkItemService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LifecycleE2ETest {

    @Inject HelpdeskYamlCaseHub caseHub;
    @Inject CaseInstanceCache caseInstanceCache;
    @Inject WorkItemService workItemService;

    @Test
    void caseHubLoaded() {
        assertNotNull(caseHub.getDefinition(),
                "helpdesk-ticket case definition should be loaded");
        assertEquals("helpdesk-ticket", caseHub.getDefinition().getName());
    }

    @Test
    void fullLifecycle_simulatorMode() {

        UUID caseId = caseHub.startCase(Map.of(
                "subject", "e2e-test laptop won't boot",
                "description", "MacBook Pro shows black screen after update",
                "customerRef", "cust-e2e",
                "status", "OPEN"));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Object category = caseHub.query(caseId, "category");
            assertNotNull(category, "Ticket should be classified");
            assertEquals("HARDWARE", category);
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Object status = caseHub.query(caseId, "status");
            assertEquals("TRIAGED", status);
        });

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var workItems = given()
                    .when().get("/api/work/items/list-all")
                    .then().statusCode(200)
                    .extract().jsonPath()
                    .getList("items.findAll { it.title.contains('e2e-test') && it.status == 'PENDING' }", Map.class);
            assertFalse(workItems.isEmpty(), "Work item should be created");
        });

        String workItemIdStr = given()
                .when().get("/api/work/items/list-all")
                .then().statusCode(200)
                .extract().jsonPath()
                .getString("items.find { it.title.contains('e2e-test') && it.status == 'PENDING' }.id");
        UUID workItemId = UUID.fromString(workItemIdStr);

        workItemService.completeFromSystem(workItemId, "hw-specialist",
                "{\"resolution\":\"BIOS reset resolved the boot loop\",\"assigneeId\":\"hw-specialist\"}");

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            var instance = caseInstanceCache.get(caseId);
            assertNotNull(instance);
            assertEquals(CaseStatus.COMPLETED, instance.getState(),
                    "Case should complete after resolution and notification");
        });
    }
}

package io.casehub.examples.manor.model;

public sealed interface ActionResult {
    record Success(String description) implements ActionResult {}
    record Failed(String reason) implements ActionResult {}
    record MovedToRoom(String roomId, String description) implements ActionResult {}
    record ItemReceived(String itemId, String description) implements ActionResult {}
    record SceneTriggered(String sceneId) implements ActionResult {}
}

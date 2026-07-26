package io.casehub.examples.manor.model;

import java.util.concurrent.CompletableFuture;

public final class PendingAction {
    private final CharacterState character;
    private final Action action;
    private final CompletableFuture<ActionResult> future = new CompletableFuture<>();

    public PendingAction(CharacterState character, Action action) {
        this.character = character;
        this.action = action;
    }

    public CharacterState character() { return character; }
    public Action action() { return action; }

    public void complete(ActionResult result) { future.complete(result); }
    public ActionResult awaitResult() throws Exception { return future.get(); }
}

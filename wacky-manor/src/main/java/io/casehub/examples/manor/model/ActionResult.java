package io.casehub.examples.manor.model;

public sealed interface ActionResult {
    String text();

    record Success(String description) implements ActionResult {
        @Override
        public String text() {return description;}
    }

    record Failed(String reason) implements ActionResult {
        @Override
        public String text() {return reason;}
    }

    record MovedToRoom(String roomId, String description) implements ActionResult {
        @Override
        public String text() {return description;}
    }

    record ItemReceived(String itemId, String description) implements ActionResult {
        @Override
        public String text() {return description;}
    }
}

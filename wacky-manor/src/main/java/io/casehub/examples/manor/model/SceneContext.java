package io.casehub.examples.manor.model;

import java.util.concurrent.CountDownLatch;

public final class SceneContext {
    private final String sceneId;
    private final CountDownLatch latch = new CountDownLatch(1);

    public SceneContext(String sceneId) {
        this.sceneId = sceneId;
    }

    public String sceneId() { return sceneId; }

    public void awaitRelease() throws InterruptedException {
        latch.await();
    }

    public void release() {
        latch.countDown();
    }
}

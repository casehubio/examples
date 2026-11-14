package io.casehub.qhorus.examples.typesystem;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.casehub.qhorus.api.store.ChannelStore;
import io.casehub.qhorus.api.store.CommitmentStore;
import io.casehub.qhorus.api.store.DataStore;
import io.casehub.qhorus.api.store.InstanceStore;
import io.casehub.qhorus.api.store.MessageStore;
import io.casehub.qhorus.persistence.memory.InMemoryChannelStore;
import io.casehub.qhorus.persistence.memory.InMemoryCommitmentStore;
import io.casehub.qhorus.persistence.memory.InMemoryDataStore;
import io.casehub.qhorus.persistence.memory.InMemoryInstanceStore;
import io.casehub.qhorus.persistence.memory.InMemoryMessageStore;
import io.quarkus.test.junit.QuarkusTest;

/** Verifies that the in-memory blocking store alternatives are selected by CDI. */
@QuarkusTest
class StoreCdiAlternativesTest {

    @Inject
    ChannelStore channelStore;

    @Inject
    MessageStore messageStore;

    @Inject
    InstanceStore instanceStore;

    @Inject
    DataStore dataStore;

    @Inject
    CommitmentStore commitmentStore;

    @Test
    void blockingStores_areInMemory() {
        assertThat(channelStore).isInstanceOf(InMemoryChannelStore.class);
        assertThat(messageStore).isInstanceOf(InMemoryMessageStore.class);
        assertThat(instanceStore).isInstanceOf(InMemoryInstanceStore.class);
        assertThat(dataStore).isInstanceOf(InMemoryDataStore.class);
        assertThat(commitmentStore).isInstanceOf(InMemoryCommitmentStore.class);
    }

}

package io.casehub.examples.helpdesk.yaml;

import io.casehub.platform.api.data.DataStore;
import io.casehub.platform.api.data.memory.InMemoryDataStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
public class DataStoreBeans {

    @Produces
    @Singleton
    DataStore dataStore() {
        return new InMemoryDataStore();
    }
}

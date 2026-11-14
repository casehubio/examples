package io.casehub.ledger.examples.otel;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordedEventLedgerEntryTest {
    @Test
    void canonicalBytesProtectEveryDistinctDomainValue() {
        var entry = new RecordedEventLedgerEntry();
        var forms = new HashSet<String>();
        for (var value : new String[]{null, "", "null", "Révision"}) {
            entry.eventName = value;
            assertTrue(forms.add(Base64.getEncoder().encodeToString(entry.canonicalBytes())),
                    "canonical bytes must distinguish " + value);
        }
    }
}

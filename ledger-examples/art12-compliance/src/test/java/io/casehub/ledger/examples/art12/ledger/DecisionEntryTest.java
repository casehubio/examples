package io.casehub.ledger.examples.art12.ledger;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionEntryTest {
    @Test
    void canonicalBytesProtectEveryDistinctDomainValue() {
        var entry = new DecisionEntry();
        var forms = new HashSet<String>();
        for (var value : new String[]{null, "", "null", "Révision"}) {
            entry.decisionCategory = value;
            assertTrue(forms.add(Base64.getEncoder().encodeToString(entry.canonicalBytes())),
                    "canonical bytes must distinguish " + value);
        }
    }
}

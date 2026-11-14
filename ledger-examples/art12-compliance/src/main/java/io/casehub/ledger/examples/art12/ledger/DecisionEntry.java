package io.casehub.ledger.examples.art12.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.casehub.ledger.jpa.JpaLedgerEntry;

@Entity
@Table(name = "art12_decision_entry")
@DiscriminatorValue("ART12_DECISION")
public class DecisionEntry extends JpaLedgerEntry {

    @Column(name = "decision_category", length = 100)
    public String decisionCategory;
    @Override
    public byte[] domainContentBytes() {
        return (decisionCategory == null ? "-" : decisionCategory.length() + ":" + decisionCategory)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}

package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManorNormFilterTest {

    @Test
    void contextRelevantNormsRankedAboveGeneral() {
        var norms = List.of(
                new SocialConfig.NormEntry("Maintain charm", 5),
                new SocialConfig.NormEntry("Never help Penelope directly", 5));
        var result = ManorNormFilter.score(norms, List.of("Penelope Pitstop"), List.of(), 10);
        assertThat(result.get(0).rule()).isEqualTo("Never help Penelope directly");
        assertThat(result.get(1).rule()).isEqualTo("Maintain charm");
    }

    @Test
    void generalNormsIncludedWithinBudget() {
        var norms = List.of(
                new SocialConfig.NormEntry("General rule", 5),
                new SocialConfig.NormEntry("Help Penelope", 3));
        var result = ManorNormFilter.score(norms, List.of("Penelope Pitstop"), List.of(), 10);
        assertThat(result).hasSize(2);
    }

    @Test
    void budgetGatesNormCount() {
        var norms = List.of(
                new SocialConfig.NormEntry("Rule A", 10),
                new SocialConfig.NormEntry("Rule B", 8),
                new SocialConfig.NormEntry("Rule C", 5));
        var result = ManorNormFilter.score(norms, List.of(), List.of(), 2);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).rule()).isEqualTo("Rule A");
        assertThat(result.get(1).rule()).isEqualTo("Rule B");
    }

    @Test
    void inventoryMatchBoostsRelevance() {
        var norms = List.of(
                new SocialConfig.NormEntry("General rule", 8),
                new SocialConfig.NormEntry("Protect the treasure", 3));
        var result = ManorNormFilter.score(norms, List.of(), List.of("treasure"), 10);
        assertThat(result.get(0).rule()).isEqualTo("Protect the treasure");
    }

    @Test
    void shortWordsIgnored() {
        var norms = List.of(
                new SocialConfig.NormEntry("Do no harm", 5));
        var result = ManorNormFilter.score(norms, List.of("HC"), List.of(), 10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).priority()).isEqualTo(5);
    }

    @Test
    void caseInsensitiveMatching() {
        var norms = List.of(
                new SocialConfig.NormEntry("never help penelope", 3),
                new SocialConfig.NormEntry("General rule", 5));
        var result = ManorNormFilter.score(norms, List.of("Penelope Pitstop"), List.of(), 10);
        assertThat(result.get(0).rule()).isEqualTo("never help penelope");
    }

    @Test
    void emptyContextReturnsTopByPriority() {
        var norms = List.of(
                new SocialConfig.NormEntry("Low", 1),
                new SocialConfig.NormEntry("High", 10),
                new SocialConfig.NormEntry("Mid", 5));
        var result = ManorNormFilter.score(norms, List.of(), List.of(), 10);
        assertThat(result.get(0).rule()).isEqualTo("High");
        assertThat(result.get(1).rule()).isEqualTo("Mid");
        assertThat(result.get(2).rule()).isEqualTo("Low");
    }

    @Test
    void multipleCognitiveSubjectsMatchIndependently() {
        var norms = List.of(
                new SocialConfig.NormEntry("Protect Penelope always", 3),
                new SocialConfig.NormEntry("Beware of Dastardly", 3),
                new SocialConfig.NormEntry("Stay calm", 3));
        var result = ManorNormFilter.score(norms,
                List.of("Penelope Pitstop", "Dick Dastardly"), List.of(), 10);
        assertThat(result.get(0).rule()).isIn("Protect Penelope always", "Beware of Dastardly");
        assertThat(result.get(1).rule()).isIn("Protect Penelope always", "Beware of Dastardly");
        assertThat(result.get(2).rule()).isEqualTo("Stay calm");
    }

    @Test
    void priorityBreaksTiesAmongBoosted() {
        var norms = List.of(
                new SocialConfig.NormEntry("Protect Penelope always", 9),
                new SocialConfig.NormEntry("Never help Penelope directly", 3));
        var result = ManorNormFilter.score(norms, List.of("Penelope Pitstop"), List.of(), 10);
        assertThat(result.get(0).rule()).isEqualTo("Protect Penelope always");
        assertThat(result.get(1).rule()).isEqualTo("Never help Penelope directly");
    }

    @Test
    void emptyNormsReturnsEmpty() {
        var result = ManorNormFilter.score(List.of(), List.of("Penelope"), List.of("key"), 10);
        assertThat(result).isEmpty();
    }
}

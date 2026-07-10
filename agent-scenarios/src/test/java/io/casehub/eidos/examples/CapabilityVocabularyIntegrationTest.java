package io.casehub.eidos.examples;

import io.casehub.eidos.api.MatchDegree;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import io.casehub.eidos.vocab.CasehubCapabilityTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CapabilityVocabularyIntegrationTest {

    @Inject VocabularyRegistry registry;

    /**
     * App-tier vocabulary that specializes foundation CasehubCapabilityTerm.DOCUMENTATION.
     * Simulates a domain-specific healthcare capability taxonomy built on top of the foundation vocabulary.
     */
    @VocabularyMetadata(uri = "urn:app:clinical:capabilities", name = "Clinical Capability Vocabulary")
    public enum ClinicalCapabilityVocab implements VocabularyTerm {
        CLINICAL_DOCUMENTATION_REVIEW("clinical-documentation-review", "Clinical Documentation Review");

        private final String value;
        private final String label;

        ClinicalCapabilityVocab(String value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override public String value() { return value; }
        @Override public String label() { return label; }

        @Override public List<VocabularyTerm> specializes() {
            // Cross-vocabulary specialization: clinical-documentation-review specializes foundation documentation term
            return List.of(CasehubCapabilityTerm.DOCUMENTATION);
        }
    }

    @Test
    void capability_vocabulary_is_registered() {
        assertThat(registry.isRegistered(CasehubCapabilityTerm.URI)).isTrue();
    }

    @Test
    void sast_review_specializes_security_code_review_and_static_analysis() {
        assertThat(CasehubCapabilityTerm.SAST_REVIEW.specializes())
            .containsExactlyInAnyOrder(
                CasehubCapabilityTerm.SECURITY_CODE_REVIEW,
                CasehubCapabilityTerm.STATIC_ANALYSIS);
    }

    @Test
    void code_review_subsumes_sast_review() {
        assertThat(registry.subsumes(CasehubCapabilityTerm.URI,
            "code-review", "sast-review")).isTrue();
    }

    @Test
    void analysis_subsumes_sast_review() {
        assertThat(registry.subsumes(CasehubCapabilityTerm.URI,
            "analysis", "sast-review")).isTrue();
    }

    @Test
    void sast_review_does_not_subsume_code_review() {
        assertThat(registry.subsumes(CasehubCapabilityTerm.URI,
            "sast-review", "code-review")).isFalse();
    }

    @Test
    void root_terms_have_no_ancestors() {
        assertThat(registry.ancestors(CasehubCapabilityTerm.URI, "code-review")).isEmpty();
        assertThat(registry.ancestors(CasehubCapabilityTerm.URI, "analysis")).isEmpty();
        assertThat(registry.ancestors(CasehubCapabilityTerm.URI, "testing")).isEmpty();
        assertThat(registry.ancestors(CasehubCapabilityTerm.URI, "documentation")).isEmpty();
    }

    @Test
    void sast_review_has_four_ancestors() {
        var ancestors = registry.ancestors(CasehubCapabilityTerm.URI, "sast-review");
        // sast-review specializes both SECURITY_CODE_REVIEW and STATIC_ANALYSIS
        // Direct specializations: SECURITY_CODE_REVIEW, STATIC_ANALYSIS
        // Transitive specializations: CODE_REVIEW (from SECURITY_CODE_REVIEW), ANALYSIS (from STATIC_ANALYSIS)
        assertThat(ancestors).hasSize(4)
            .extracting(VocabularyTerm::value)
            .containsExactlyInAnyOrder("security-code-review", "static-analysis", "code-review", "analysis");
    }

    @Test
    void static_analysis_has_analysis_as_ancestor() {
        var ancestors = registry.ancestors(CasehubCapabilityTerm.URI, "static-analysis");
        assertThat(ancestors).hasSize(1)
            .extracting(VocabularyTerm::value)
            .contains("analysis");
    }

    @Test
    void security_code_review_has_code_review_as_ancestor() {
        var ancestors = registry.ancestors(CasehubCapabilityTerm.URI, "security-code-review");
        assertThat(ancestors).hasSize(1)
            .extracting(VocabularyTerm::value)
            .contains("code-review");
    }

    @Test
    void all_capability_terms_are_resolvable() {
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "code-review")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "security-code-review")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "performance-code-review")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "sast-review")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "analysis")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "static-analysis")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "testing")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "documentation")).isPresent();
    }

    @Test
    void capability_aliases_are_resolvable() {
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "static-app-security-test"))
            .isPresent()
            .get()
            .extracting(VocabularyTerm::value)
            .isEqualTo("sast-review");

        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "static-code-analysis"))
            .isPresent()
            .get()
            .extracting(VocabularyTerm::value)
            .isEqualTo("static-analysis");
    }

    // --- Cross-vocabulary subsumption integration tests ---

    @Test
    void cross_vocabulary_subsumes_foundation_documentation_subsumes_clinical_documentation_review() {
        // Register the clinical vocabulary
        registry.register(ClinicalCapabilityVocab.class);

        // Foundation term subsumes app-tier specialization across vocabularies
        // Query in app vocab context: foundation "documentation" subsumes app "clinical-documentation-review"
        assertThat(registry.subsumes("urn:app:clinical:capabilities",
            "documentation", "clinical-documentation-review"))
            .isTrue();
    }

    @Test
    void cross_vocabulary_subsumes_clinical_documentation_review_does_not_subsume_documentation() {
        registry.register(ClinicalCapabilityVocab.class);

        // App-tier specialization does NOT subsume foundation term
        assertThat(registry.subsumes("urn:app:clinical:capabilities",
            "clinical-documentation-review", "documentation"))
            .isFalse();
    }

    @Test
    void cross_vocabulary_match_foundation_to_app_tier_returns_plugin() {
        registry.register(ClinicalCapabilityVocab.class);

        // Querying for foundation term when app declares specialization → Plugin match
        // Query in app vocab context: declared "documentation", requested "clinical-documentation-review"
        var match = registry.match("urn:app:clinical:capabilities",
            "documentation", "clinical-documentation-review");

        assertThat(match).isInstanceOf(MatchDegree.Plugin.class);
        assertThat(((MatchDegree.Plugin) match).depth()).isEqualTo(1);
    }

    @Test
    void cross_vocabulary_match_app_tier_to_foundation_returns_specialization() {
        registry.register(ClinicalCapabilityVocab.class);

        // Querying for app-tier term when foundation declares base → Specialization match
        // Query in app vocab context: declared "clinical-documentation-review", requested "documentation"
        var match = registry.match("urn:app:clinical:capabilities",
            "clinical-documentation-review", "documentation");

        assertThat(match).isInstanceOf(MatchDegree.Specialization.class);
        assertThat(((MatchDegree.Specialization) match).depth()).isEqualTo(1);
    }

    @Test
    void cross_vocabulary_ancestors_clinical_documentation_review_has_documentation_ancestor() {
        registry.register(ClinicalCapabilityVocab.class);

        var ancestors = registry.ancestors("urn:app:clinical:capabilities", "clinical-documentation-review");

        assertThat(ancestors).hasSize(1)
            .extracting(VocabularyTerm::value)
            .contains("documentation");

        // Verify the ancestor is the foundation term
        var ancestor = ancestors.get(0);
        assertThat(ancestor).isEqualTo(CasehubCapabilityTerm.DOCUMENTATION);
    }

    @Test
    void cross_vocabulary_descendants_documentation_includes_clinical_documentation_review() {
        registry.register(ClinicalCapabilityVocab.class);

        var descendants = registry.descendants(CasehubCapabilityTerm.URI, "documentation");

        // Foundation documentation term should have the app-tier clinical term as descendant
        assertThat(descendants)
            .extracting(VocabularyTerm::value)
            .contains("clinical-documentation-review");
    }

    @Test
    void cross_vocabulary_expandForMatchingByVocabulary_groups_cross_vocabulary_hierarchy() {
        registry.register(ClinicalCapabilityVocab.class);

        // Start with clinical-documentation-review — should expand to include foundation documentation
        var expanded = registry.expandForMatchingByVocabulary("clinical-documentation-review");

        assertThat(expanded).hasSize(2);

        // Group for foundation vocab should contain documentation
        assertThat(expanded).containsKey(CasehubCapabilityTerm.URI);
        assertThat(expanded.get(CasehubCapabilityTerm.URI))
            .contains("documentation");

        // Group for app vocab should contain clinical-documentation-review
        assertThat(expanded).containsKey("urn:app:clinical:capabilities");
        assertThat(expanded.get("urn:app:clinical:capabilities"))
            .contains("clinical-documentation-review");
    }

    @Test
    void cross_vocabulary_expandForMatchingByVocabulary_from_foundation_includes_app_tier_descendants() {
        registry.register(ClinicalCapabilityVocab.class);

        // Start with foundation documentation — should expand to include app-tier clinical specialization
        var expanded = registry.expandForMatchingByVocabulary("documentation");

        assertThat(expanded).hasSize(2);

        // Foundation group contains documentation
        assertThat(expanded.get(CasehubCapabilityTerm.URI))
            .contains("documentation");

        // App-tier group contains clinical-documentation-review
        assertThat(expanded.get("urn:app:clinical:capabilities"))
            .contains("clinical-documentation-review");
    }
}

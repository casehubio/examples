package io.casehub.examples.manor.experiment;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EvalFilterTest {

    @Test
    void no_filter_includes_everything() {
        var filter = EvalFilter.from(Optional.empty(), Optional.empty());
        assertTrue(filter.includesCharacter("peter-perfect"));
        assertTrue(filter.includesCharacter("penelope-pitstop"));
        assertTrue(filter.includesLayer("baseline"));
        assertTrue(filter.includesLayer("jungian"));
    }

    @Test
    void character_filter_includes_only_named() {
        var filter = EvalFilter.from(Optional.of("peter-perfect"), Optional.empty());
        assertTrue(filter.includesCharacter("peter-perfect"));
        assertFalse(filter.includesCharacter("penelope-pitstop"));
        assertFalse(filter.includesCharacter("hooded-claw"));
        assertTrue(filter.includesLayer("baseline"));
    }

    @Test
    void layer_filter_includes_only_named() {
        var filter = EvalFilter.from(Optional.empty(), Optional.of("jungian,composite"));
        assertTrue(filter.includesCharacter("peter-perfect"));
        assertTrue(filter.includesLayer("jungian"));
        assertTrue(filter.includesLayer("composite"));
        assertFalse(filter.includesLayer("baseline"));
        assertFalse(filter.includesLayer("belbin"));
    }

    @Test
    void both_filters_combine() {
        var filter = EvalFilter.from(
                Optional.of("peter-perfect,penelope-pitstop"),
                Optional.of("jungian,composite"));
        assertTrue(filter.includesCharacter("peter-perfect"));
        assertTrue(filter.includesCharacter("penelope-pitstop"));
        assertFalse(filter.includesCharacter("hooded-claw"));
        assertTrue(filter.includesLayer("jungian"));
        assertFalse(filter.includesLayer("baseline"));
    }

    @Test
    void empty_string_treated_as_no_filter() {
        var filter = EvalFilter.from(Optional.of(""), Optional.of(""));
        assertTrue(filter.includesCharacter("peter-perfect"));
        assertTrue(filter.includesLayer("baseline"));
    }

    @Test
    void whitespace_trimmed() {
        var filter = EvalFilter.from(
                Optional.of(" peter-perfect , penelope-pitstop "),
                Optional.of(" jungian "));
        assertTrue(filter.includesCharacter("peter-perfect"));
        assertTrue(filter.includesCharacter("penelope-pitstop"));
        assertFalse(filter.includesCharacter("hooded-claw"));
        assertTrue(filter.includesLayer("jungian"));
        assertFalse(filter.includesLayer("composite"));
    }
}

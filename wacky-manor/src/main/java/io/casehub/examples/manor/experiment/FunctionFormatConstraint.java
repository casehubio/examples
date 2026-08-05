package io.casehub.examples.manor.experiment;

import io.casehub.eidos.api.AgentDisposition;

import java.util.Map;

public final class FunctionFormatConstraint {
    private FunctionFormatConstraint() {}

    private static final Map<String, String> FORMAT_CONSTRAINTS = Map.of(
            "te", "Structure your responses as numbered action plans with explicit criteria for success",
            "ti", "Present your reasoning as a logical chain: premise → analysis → conclusion",
            "fe", "Frame your responses around how actions affect the group — who benefits, who is harmed, what’s the relational impact",
            "fi", "Ground your responses in your core values — state what matters to you and why before deciding",
            "se", "Focus on what’s immediately actionable — concrete objects, present dangers, physical options",
            "si", "Reference what you’ve seen before, established procedures, and proven approaches",
            "ni", "Converge to a single strategic insight — one prediction, one pattern, one conclusion",
            "ne", "Explore multiple possibilities before converging — what-ifs, alternatives, connections"
    );

    private static final Map<String, String> COGNITIVE_APPROACHES = Map.of(
            "te", "As a systematic strategic thinker, prioritise structured execution over creative exploration. Evaluate each option against your objectives. Organise your approach into clear steps.",
            "ti", "As an analytical thinker, build your reasoning from first principles. Seek internal consistency and precision. Question assumptions before acting.",
            "fe", "As a group-aware communicator, consider how your actions affect everyone present. Seek consensus where possible. Frame decisions in terms of relational impact.",
            "fi", "As a value-driven individual, check your actions against your deeply held principles. Choose what feels authentically right over what others expect.",
            "se", "As a hands-on pragmatist, focus on what is immediately in front of you. Act on concrete details and present realities. Deliver practical solutions.",
            "si", "As a methodical practitioner, draw on established procedures and past experience. Follow proven approaches step by step.",
            "ni", "As a pattern-focused strategist, look for the deeper meaning beneath surface events. Converge on a single insight or prediction.",
            "ne", "As an idea-generating explorer, brainstorm multiple possibilities and connections. Open up alternatives before narrowing down."
    );

    private static final Map<String, String> REASONING_INSTRUCTIONS = Map.of(
            "te", "Think through your response using systematic analysis — what options exist, which is optimal, why.",
            "ti", "Think through your response using logical analysis from first principles — what is the underlying structure, what follows logically.",
            "fe", "Think through your response by considering group dynamics — who is affected, what maintains harmony, what serves the collective.",
            "fi", "Think through your response by checking against your core values — what feels right, what aligns with who you are.",
            "se", "Think through your response by scanning the immediate environment — what is actionable right now, what concrete details matter.",
            "si", "Think through your response by recalling precedent — what has worked before, what established procedure applies.",
            "ni", "Think through your response by converging on one deep insight — what is the single pattern or prediction that explains this situation.",
            "ne", "Think through your response by exploring possibilities — what connections, alternatives, and what-ifs emerge from this situation."
    );

    private static final Map<String, String> THINKING_DESCRIPTIONS = Map.of(
            "te", "(describe your systematic analysis — what options exist, which is optimal, why; not shown to others)",
            "ti", "(reason from first principles — what is the logical structure, what follows; not shown to others)",
            "fe", "(consider group impact — who is affected, what maintains harmony; not shown to others)",
            "fi", "(check against your core values — what feels right, what aligns with who you are; not shown to others)",
            "se", "(scan the immediate environment — what is actionable, what concrete details matter; not shown to others)",
            "si", "(recall precedent — what has worked before, what established procedure applies; not shown to others)",
            "ni", "(converge on one insight — what is the single pattern or prediction here; not shown to others)",
            "ne", "(explore possibilities — what connections and alternatives emerge; not shown to others)"
    );

    public static String forDominant(String function) {
        return FORMAT_CONSTRAINTS.get(function.toLowerCase());
    }

    public static String cognitiveApproach(AgentDisposition disposition) {
        return DominantFunction.of(disposition)
                .map(fn -> COGNITIVE_APPROACHES.get(fn.toLowerCase()))
                .orElse(null);
    }

    public static String reasoningInstruction(AgentDisposition disposition) {
        return DominantFunction.of(disposition)
                .map(fn -> REASONING_INSTRUCTIONS.get(fn.toLowerCase()))
                .orElse(null);
    }

    public static String thinkingDescription(AgentDisposition disposition) {
        return DominantFunction.of(disposition)
                .map(fn -> THINKING_DESCRIPTIONS.get(fn.toLowerCase()))
                .orElse(null);
    }
}

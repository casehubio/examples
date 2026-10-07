package io.casehub.examples.manor.engine;

import io.casehub.blocks.memory.ArousalScorer;
import io.casehub.neocortex.memory.Memory;
import io.casehub.neocortex.memory.experience.ContentScorer;
import io.casehub.neocortex.memory.experience.GraduationContext;
import io.casehub.neocortex.memory.experience.GraduationScorer;
import io.casehub.neocortex.memory.experience.ScoreableContent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ManorGraduationScorer implements GraduationScorer {

    private final ContentScorer compositeScorer;

    public ManorGraduationScorer() {
        this.compositeScorer = ContentScorer.composite(List.of(
            ContentScorer.weighted(new ArousalScorer(), 0.3),
            ContentScorer.weighted(new ActionImportanceScorer(), 0.4)));
    }

    @Override
    public double score(Memory memory, GraduationContext context) {
        String eventType = memory.attributes().getOrDefault(
                io.casehub.neocortex.memory.experience.ExperienceAttributeKeys.EVENT_TYPE, "");
        if ("formative".equals(eventType)) {
            double base = memory.confidence() != null ? memory.confidence().value() : 0.8;
            String salienceStr = memory.attributes().get(
                    io.casehub.neocortex.memory.experience.FormativeAttributeKeys.SALIENCE_MULTIPLIER);
            double salience = salienceStr != null ? Double.parseDouble(salienceStr) : 1.0;
            return Math.min(1.0, base * salience);
        }

        ScoreableContent content      = ScoreableContent.fromMemory(memory);
        double           contentScore = compositeScorer.score(content);
        double           confidence   = memory.confidence() != null ? memory.confidence().value() : 0.5;
        return Math.clamp(contentScore * 0.7 + confidence * 0.3, 0.0, 1.0);
    }
}

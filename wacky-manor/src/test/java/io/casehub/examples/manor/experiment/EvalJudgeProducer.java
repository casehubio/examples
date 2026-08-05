package io.casehub.examples.manor.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import io.casehub.eidos.eval.BriefingCoherenceJudge;
import io.casehub.eidos.eval.FunctionActivationJudge;
import io.casehub.eidos.eval.MbtiAlignmentJudge;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class EvalJudgeProducer {

    @Produces
    @ApplicationScoped
    MbtiAlignmentJudge mbtiAlignmentJudge(@Any Instance<ChatModel> models, ObjectMapper mapper) {
        return new MbtiAlignmentJudge(models, mapper);
    }

    @Produces
    @ApplicationScoped
    FunctionActivationJudge functionActivationJudge(@Any Instance<ChatModel> models, ObjectMapper mapper) {
        return new FunctionActivationJudge(models, mapper);
    }

    @Produces
    @ApplicationScoped
    BriefingCoherenceJudge briefingCoherenceJudge(@Any Instance<ChatModel> models, ObjectMapper mapper) {
        return new BriefingCoherenceJudge(models, mapper);
    }
}

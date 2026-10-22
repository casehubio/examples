package io.casehub.examples.manor.agent;

import io.casehub.agency.perception.observation.affordance.ObservationSection;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.cognitive.index.CognitiveDefaults;
import io.casehub.neocortex.memory.Memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class CharacterCognition {

    private final String                                                agentId;
    private final AgentExperienceService                                experienceService;
    private final CognitiveDefaults                                     cognitiveDefaults;
    private final SocialConfig                                          socialConfig;
    private final List<AgentConstraint>                                 constraints;
    private final io.casehub.neocortex.cognitive.index.CognitiveProfile cognitiveProfile;
    private final ManorContextStrategy                                  contextStrategy;
    private final io.casehub.neocortex.cognition.core.CognitionCore        cognitionCore;
    private final ManorCognitiveSeeder.SeedResult                       seedResult;
    private final String                                                tenantId;
    private final io.casehub.neocortex.mindmap.MindMapStore             mindMapStore;
    private final io.casehub.engine.trust.TrustEvolutionConfig          trustEvolutionConfig;
    private final io.casehub.neocortex.caps.SituationClassifier          situationClassifier;
    private       boolean                                                lastClassifierActive = true;
    private final java.util.Map<String, io.casehub.neocortex.mindmap.intelligence.tom.TheoryOfMindEngine> tomEngines = new java.util.HashMap<>();
    private final RegulationTracker regulationTracker = new RegulationTracker();


    public CharacterCognition(String agentId, AgentExperienceService experienceService) {
        this(agentId, experienceService, null, SocialConfig.empty(), List.of(),
             null, new ManorContextStrategy(), null, null, null, null, null, null);
    }

    public CharacterCognition(String agentId, AgentExperienceService experienceService,
                              CognitiveDefaults cognitiveDefaults, SocialConfig socialConfig,
                              List<AgentConstraint> constraints) {
        this(agentId, experienceService, cognitiveDefaults, socialConfig, constraints,
             null, new ManorContextStrategy(), null, null, null, null, null, null);
    }

    public CharacterCognition(String agentId, AgentExperienceService experienceService,
                              CognitiveDefaults cognitiveDefaults, SocialConfig socialConfig,
                              List<AgentConstraint> constraints,
                              io.casehub.neocortex.cognitive.index.CognitiveProfile cognitiveProfile,
                              ManorContextStrategy contextStrategy,
                              io.casehub.neocortex.cognition.core.CognitionCore cognitionCore,
                              ManorCognitiveSeeder.SeedResult seedResult,
                              String tenantId,
                              io.casehub.neocortex.mindmap.MindMapStore mindMapStore,
                              io.casehub.engine.trust.TrustEvolutionConfig trustEvolutionConfig,
                              io.casehub.neocortex.caps.SituationClassifier situationClassifier) {
        this.agentId                = agentId;
        this.experienceService      = experienceService;
        this.cognitiveDefaults      = cognitiveDefaults;
        this.socialConfig           = socialConfig != null ? socialConfig : SocialConfig.empty();
        this.constraints            = constraints != null ? List.copyOf(constraints) : List.of();
        this.cognitiveProfile       = cognitiveProfile;
        this.contextStrategy        = contextStrategy != null ? contextStrategy : new ManorContextStrategy();
        this.cognitionCore          = cognitionCore;
        this.seedResult             = seedResult;
        this.tenantId               = tenantId;
        this.mindMapStore           = mindMapStore;
        this.trustEvolutionConfig   = trustEvolutionConfig;
        this.situationClassifier    = situationClassifier;
    }

    public String agentId() {return agentId;}

    public double computeImportance(ActionType action) {
        if (action == null) {return 0.5;}
        return switch (action) {
            case STEAL -> 0.9;
            case USE -> 0.8;
            case TAKE, GIVE, PULL_ASIDE -> 0.7;
            case INTERACT -> 0.6;
            case MOVE -> 0.3;
            case LOOK -> 0.2;
            case WAIT -> 0.1;
        };
    }

    public void recordExperience(String room, String description, String thinking,
                                 double importance, String targetAgentId, int tick) {
        if (experienceService != null) {
            experienceService.ingest(agentId, room, description, thinking, importance, targetAgentId, tick);
        }
    }

    public List<Memory> recallMemories(int limit) {
        return experienceService != null ? experienceService.recall(agentId, limit) : List.of();
    }

    public List<Memory> recallReflections(int limit) {
        return experienceService != null ? experienceService.recallReflections(agentId, limit) : List.of();
    }

    public List<Memory> recallRelationships(String otherId, int limit) {
        return experienceService != null ? experienceService.recallRelationships(agentId, otherId, limit) : List.of();
    }

    public void observeCharacterState(String characterId,
                                      io.casehub.neocortex.mindmap.intelligence.tom.BehaviorType behavior,
                                      io.casehub.neocortex.mindmap.intelligence.tom.ThreatLevel threat) {
        var engine = tomEngines.computeIfAbsent(characterId,
                                                k -> new io.casehub.neocortex.mindmap.intelligence.tom.TheoryOfMindEngine());
        engine.observeBehavior(behavior);
        engine.observeThreat(threat);
        regulationTracker.recordPosterior(characterId, engine.inferEmotionalState());
    }

    private List<ObservationSection> renderTheoryOfMind(Collection<String> nearbyAgentIds,
                                                        Map<String, String> agentNames) {
        var sections = new ArrayList<ObservationSection>();
        for (var id : nearbyAgentIds) {
            var engine = tomEngines.get(id);
            if (engine == null) {continue;}

            double[] state      = engine.inferEmotionalState();
            double   distressed = state[io.casehub.neocortex.mindmap.intelligence.tom.EmotionalValence.DISTRESSED.ordinal()];

            if (distressed < 0.35) {continue;}

            String reading = readOtherModel(id);
            if (reading == null || reading.isBlank()) {continue;}

            String name = agentNames.getOrDefault(id, id);

            var trajectory = regulationTracker.computeTrajectory(id);
            if (trajectory.isPresent()) {
                String velocityText = switch (trajectory.get().trajectory()) {
                    case IMPROVING -> "Something in your reading of " + name + " is shifting — the distress you sensed is easing.";
                    case STABLE -> "Whatever you have tried has not reached " + name + ". The distress you sense has not shifted.";
                    case WORSENING -> "It is getting worse. The distress you sense in " + name + " is deepening.";
                };
                reading = reading + "\n\n" + velocityText;
            }

            sections.add(ObservationSection.text("What You Sense About " + name, reading));
        }
        return sections;
    }

    private String readOtherModel(String targetId) {
        if (mindMapStore == null || tenantId == null) {return null;}
        var node = mindMapStore.listSubgraphs(tenantId).stream()
                               .filter(s -> "cognitive".equals(s.type()))
                               .flatMap(s -> mindMapStore.nodesIn(s.id(), tenantId).stream())
                               .filter(n -> n.traits().contains("OtherModelDerived"))
                               .filter(n -> agentId.equals(n.properties().get("agent-id")))
                               .filter(n -> targetId.equals(n.properties().get("target-id")))
                               .findFirst()
                               .orElse(null);
        if (node == null) {return null;}

        var parts   = new java.util.ArrayList<String>();
        var sensing = node.properties().get("empathic-sensing");
        if (sensing != null && !sensing.isBlank()) {parts.add(sensing);}
        var values = node.properties().get("inferred-values");
        if (values != null && !values.isBlank()) {parts.add(values);}
        var sensitivities = node.properties().get("appraisal-sensitivities");
        if (sensitivities != null && !sensitivities.isBlank()) {parts.add(sensitivities);}
        var bond = node.properties().get("relational-understanding");
        if (bond != null && !bond.isBlank()) {parts.add(bond);}

        // backward compat: single-field empathic-reading from earlier version
        if (parts.isEmpty()) {
            var legacy = node.properties().get("empathic-reading");
            if (legacy != null && !legacy.isBlank()) {parts.add(legacy);}
        }

        return parts.isEmpty() ? null : String.join("\n\n", parts);
    }

    private List<String> readPrioritySignatures(List<io.casehub.neocortex.mindmap.MindMapSubgraph> subgraphs,
                                                List<SocialConfig.Drive> filteredDrives) {
        var activeDriveTypes = filteredDrives.stream()
                                             .map(SocialConfig.Drive::type)
                                             .collect(java.util.stream.Collectors.toSet());

        return subgraphs.stream()
                        .filter(s -> "cognitive".equals(s.type()))
                        .flatMap(s -> mindMapStore.nodesIn(s.id(), tenantId).stream())
                        .filter(n -> n.traits().contains("PrioritySignatureDerived"))
                        .filter(n -> agentId.equals(n.properties().get("agent-id")))
                        .filter(n -> {
                            String driveA = n.properties().get("drive-a");
                            String driveB = n.properties().get("drive-b");
                            return activeDriveTypes.contains(driveA) && activeDriveTypes.contains(driveB);
                        })
                        .map(n -> n.properties().get("priority-signature"))
                        .filter(s -> s != null && !s.isBlank())
                        .toList();
    }


    public io.casehub.neocortex.caps.SituationContext classifySituation(String situationText, Collection<String> nearbyAgentIds) {
        if (situationClassifier == null || situationText == null || situationText.isBlank()) {
            return io.casehub.neocortex.caps.SituationContext.EMPTY;
        }
        try {
            var metadata    = buildSituationMetadata(nearbyAgentIds);
            var activations = situationClassifier.classify(situationText, metadata);
            return io.casehub.neocortex.caps.SituationContext.from(activations);
        } catch (Exception e) {
            return io.casehub.neocortex.caps.SituationContext.EMPTY;
        }
    }

    // Section ordering follows Lazarus (1991) appraisal process flow
    public List<ObservationSection> renderCognitiveSections(
            CharacterState character,
            Collection<String> nearbyAgentIds,
            Map<String, String> agentNames) {
        return renderCognitiveSections(character, nearbyAgentIds, agentNames,
                                       classifySituation(character.lastActionResult(), nearbyAgentIds));
    }

    public List<ObservationSection> renderCognitiveSections(
            CharacterState character,
            Collection<String> nearbyAgentIds,
            Map<String, String> agentNames,
            io.casehub.neocortex.caps.SituationContext situationContext) {
        var sections = new ArrayList<ObservationSection>();

        if (!socialConfig.tendencies().isEmpty()) {
            sections.add(ObservationSection.items(
                    "Your Behavioral Tendencies", null, socialConfig.tendencies()));
        }

        if (mindMapStore != null && tenantId != null) {
            var beliefSubgraphName = ManorCognitiveSeeder.subgraphName(agentId);
            var subgraphs          = mindMapStore.listSubgraphs(tenantId);
            var beliefSg = subgraphs.stream()
                                    .filter(sg -> beliefSubgraphName.equals(sg.name()))
                                    .findFirst();
            if (beliefSg.isPresent()) {
                var nodes = mindMapStore.nodesIn(beliefSg.get().id(), tenantId);
                var beliefNodes = nodes.stream()
                                       .filter(n -> n.traits().contains("Belieflike"))
                                       .toList();
                if (!beliefNodes.isEmpty()) {
                    var revisedKeys = new java.util.HashSet<String>();
                    var beliefs     = new java.util.ArrayList<io.casehub.agency.deliberation.belief.Belief<String>>();
                    for (var node : beliefNodes) {
                        var key          = node.property("subject").orElse(node.name());
                        var value        = node.name();
                        int entrenchment = (int) (node.confidence().value() * 10);
                        beliefs.add(io.casehub.agency.deliberation.belief.Belief.of(key, value, entrenchment));
                        if ("belief-revision".equals(node.provenance())) {
                            revisedKeys.add(key);
                        }
                    }
                    sections.add(io.casehub.agency.perception.observation.affordance
                                         .CognitiveObservationSections.beliefsSection(beliefs, revisedKeys));
                }
            }
        } else if (!socialConfig.initialBeliefs().isEmpty()) {
            var items = socialConfig.initialBeliefs().stream()
                                    .map(SocialConfig.InitialBelief::value)
                                    .toList();
            sections.add(ObservationSection.items("Your Beliefs", null, items));
        }

        var selectedNorms = contextStrategy.selectNorms(socialConfig.norms(), agentNames.values(), character.inventory());
        if (!selectedNorms.isEmpty()) {
            var items = selectedNorms.stream()
                                     .map(SocialConfig.NormEntry::rule)
                                     .toList();
            sections.add(ObservationSection.items("Social Rules", null, items));
        }

        sections.addAll(renderSocialAwareness(nearbyAgentIds, agentNames));

        sections.addAll(renderTheoryOfMind(nearbyAgentIds, agentNames));

        if (!socialConfig.personalityFacets().isEmpty()) {
            sections.add(renderPersonalityFacets());
        }

        var activatedCategories = (cognitionCore != null)
                                  ? cognitionCore.activatedDriveCategories()
                                  : java.util.List.<String>of();
        var internalState = renderInternalState(activatedCategories, character.lastActionResult(), nearbyAgentIds, situationContext);
        if (internalState != null) {
            sections.add(internalState);
        }

        if (!socialConfig.formationMemories().isEmpty()) {
            sections.add(renderFormationMemories());
        }

        if (socialConfig.substrate() != null && !socialConfig.substrate().isBlank()) {
            sections.add(ObservationSection.text("What Shaped You", socialConfig.substrate()));
        }

        if (!socialConfig.relationalSchemas().isEmpty() && !nearbyAgentIds.isEmpty()) {
            var schemaSection = renderRelationalSchemas(nearbyAgentIds, agentNames);
            if (schemaSection != null) {
                sections.add(schemaSection);
            }
        }

        if (socialConfig.attachment() != null) {
            sections.add(renderAttachmentStyle());
        }

        var trustSections = renderTrustSections();
        if (!trustSections.isEmpty()) {
            sections.addAll(trustSections);
        }

        if (cognitionCore != null && tenantId != null) {
            var activatedDrives = cognitionCore.activatedDriveCategories();
            var ctx = new io.casehub.neocortex.cognition.prompt.CognitionRenderContext(
                    agentId, tenantId, null, activatedDrives);
            for (var section : cognitionCore.promptSections()) {
                if (section.blockTag() != null) {continue;}
                var text = section.render(ctx);
                if (text != null && !text.isBlank()) {
                    sections.add(adaptPromptSection(text));
                }
            }
        }

        var personaSection = PersonaActivationSection.resolve(
                socialConfig.personaConstraint(), nearbyAgentIds);
        if (personaSection != null) {
            sections.add(personaSection);
        }

        return sections;
    }


    public List<String> renderTaggedSections() {
        if (cognitionCore == null || tenantId == null) {return List.of();}
        var activatedDrives = cognitionCore.activatedDriveCategories();
        var ctx = new io.casehub.neocortex.cognition.prompt.CognitionRenderContext(
                agentId, tenantId, null, activatedDrives);
        var tagged = new ArrayList<String>();
        for (var section : cognitionCore.promptSections()) {
            if (section.blockTag() == null) {continue;}
            var text = section.render(ctx);
            if (text != null && !text.isBlank()) {
                tagged.add(text);
            }
        }
        return tagged;
    }

    private ObservationSection renderFormationMemories() {
        var sb = new StringBuilder();
        sb.append("These are the experiences that made you who you are.\n\n");
        // Salience ordering: abs(valence) + arousal — Menon (2015) salience network
        var sorted = socialConfig.formationMemories().stream()
                                 .sorted(java.util.Comparator.comparingDouble(
                                         (SocialConfig.FormationMemory m) -> Math.abs(m.pleasure()) + m.arousal()).reversed())
                                 .toList();
        for (var mem : sorted) {
            sb.append("**Age ").append(mem.age()).append(":** ")
              .append(FormationMemoryParser.stripAnnotations(mem.episode())).append("\n\n");
        }
        return ObservationSection.text("Where You Come From", sb.toString().strip());
    }

    private ObservationSection renderPersonalityFacets() {
        var sb = new StringBuilder();
        for (var facet : socialConfig.personalityFacets()) {
            if (!facet.origin().isEmpty()) {
                sb.append(facet.origin()).append(" ");
            }
        }
        return ObservationSection.text("Who You Are", sb.toString().strip());
    }

    ObservationSection renderInternalState(List<String> activatedCategories, String situationText) {
        return renderInternalState(activatedCategories, situationText, List.of());
    }

    ObservationSection renderInternalState(List<String> activatedCategories, String situationText,
                                           Collection<String> nearbyAgentIds) {
        var ctx = classifySituation(situationText, nearbyAgentIds);
        return renderInternalState(activatedCategories, situationText, nearbyAgentIds, ctx);
    }

    ObservationSection renderInternalState(List<String> activatedCategories, String situationText,
                                           Collection<String> nearbyAgentIds,
                                           io.casehub.neocortex.caps.SituationContext situationContext) {
        var drives                = resolveAdaptedDrives();
        var generatedDescriptions = readGeneratedDriveDescriptions();

        java.util.function.Function<SocialConfig.Drive, String> descriptionFor = d -> {
            String generated = generatedDescriptions.get(d.type());
            return generated != null ? generated : d.description();
        };

        var eligible = drives.stream()
                             .filter(d -> d.intensity() > 0.2 && !descriptionFor.apply(d).isBlank())
                             .toList();

        var filtered = appraisalFilterDrives(eligible, situationContext, nearbyAgentIds);

        var driveDescriptions = filtered.stream()
                                        .sorted(java.util.Comparator.comparingDouble(SocialConfig.Drive::intensity).reversed())
                                        .map(descriptionFor)
                                        .toList();

        var parts = new java.util.ArrayList<>(driveDescriptions);

        int memoryCount       = 0;
        int attractorCount    = 0;
        int diffTextCount     = 0;
        int selfStandardCount = 0;
        int prioritySigCount  = 0;

        if (mindMapStore != null && tenantId != null) {
            var subgraphs = mindMapStore.listSubgraphs(tenantId);

            var selfStandards = readSelfStandards(subgraphs);
            for (var d : filtered) {
                String standard = selfStandards.get(d.type());
                if (standard != null && !standard.isBlank()) {
                    parts.add(standard);
                    selfStandardCount++;
                }
            }

            var primedMemories = selectSituationPrimedMemories(situationContext, activatedCategories);
            if (!primedMemories.isEmpty()) {
                parts.addAll(primedMemories);
                memoryCount = primedMemories.size();
            }

            var attractorLines = subgraphs.stream()
                                          .filter(s -> "behavioral".equals(s.type()))
                                          .flatMap(s -> mindMapStore.nodesIn(s.id(), tenantId).stream())
                                          .filter(n -> n.traits().contains("CapsGenerated"))
                                          .filter(n -> agentId.equals(n.properties().get("agent-id")))
                                          .filter(n -> {
                                              var str = n.properties().get("strength");
                                              return str != null && Double.parseDouble(str) > 0.1;
                                          })
                                          .sorted(java.util.Comparator.comparingDouble(
                                                  (io.casehub.neocortex.mindmap.MindMapNode n) -> {
                                                      var str = n.properties().get("strength");
                                                      return str != null ? Double.parseDouble(str) : 0.0;
                                                  }).reversed())
                                          .limit(5)
                                          .map(this::renderAttractorAsFelt)
                                          .toList();
            if (!attractorLines.isEmpty()) {
                parts.addAll(attractorLines);
                attractorCount = attractorLines.size();
            }

            if (lastClassifierActive) {
                var activeDriveTypes = filtered.stream().map(SocialConfig.Drive::type).collect(java.util.stream.Collectors.toSet());
                var diffTexts        = readDriveDifferentiation(subgraphs, activeDriveTypes);
                parts.addAll(diffTexts);
                diffTextCount = diffTexts.size();
            }

            if (filtered.size() >= 2) {
                var prioritySigs = readPrioritySignatures(subgraphs, filtered);
                parts.addAll(prioritySigs);
                prioritySigCount = prioritySigs.size();
            }
        }

        if (parts.isEmpty()) {return null;}

        var driveNames = filtered.stream().map(SocialConfig.Drive::type).collect(java.util.stream.Collectors.joining(", "));
        System.out.printf("[observation-assembly] %s | drives=%d [%s] | selfStandards=%d | diffTexts=%d | prioritySigs=%d | memories=%d | attractors=%d | totalParts=%d%n",
                          agentId, filtered.size(), driveNames, selfStandardCount, diffTextCount, prioritySigCount, memoryCount, attractorCount, parts.size());

        return ObservationSection.text("What Pulls at You", String.join("\n\n", parts));
    }


    // Attractor intensity labeling — Russell (1980) circumplex distance-from-origin
    private String renderAttractorAsFelt(io.casehub.neocortex.mindmap.MindMapNode node) {
        var    strengthStr      = node.properties().get("strength");
        double strength         = strengthStr != null ? Double.parseDouble(strengthStr) : 0.0;
        var    prevStr          = node.properties().get("previous-strength");
        var    sources          = node.properties().get("source-names");
        var    personalityLabel = node.properties().get("personality-label");

        String name = personalityLabel != null ? personalityLabel : node.name();

        String label = strength > 0.7 ? "Something deep-rooted"
                                      : strength > 0.4 ? "Something"
                                                       : "A faint thread of something";

        var sb = new StringBuilder();
        sb.append(label).append(" — a ").append(name);
        if (prevStr != null) {
            double prev = Double.parseDouble(prevStr);
            if (strength > prev + 0.05) {sb.append(", growing stronger");} else if (strength < prev - 0.05) {
                sb.append(", starting to fade");
            }
        }
        if (sources != null && !sources.isBlank()) {
            String clean = sources.replaceAll("\\{\\{serves: [\\w-]+\\|\\w+: ", "").replace("}}", "");
            sb.append(" — rooted in ").append(clean.trim());
        }
        sb.append(".");
        return sb.toString();
    }


    // Easterbrook (1959): irrelevant content degrades attentional performance
    private List<String> readDriveDifferentiation(java.util.List<io.casehub.neocortex.mindmap.MindMapSubgraph> subgraphs, java.util.Set<String> activeDriveTypes) {
        var allCompetition = subgraphs.stream()
                                      .filter(s -> "cognitive".equals(s.type()))
                                      .flatMap(s -> mindMapStore.nodesIn(s.id(), tenantId).stream())
                                      .filter(n -> n.traits().contains("DriveCompetitionInferred"))
                                      .filter(n -> agentId.equals(n.properties().get("agent-id")))
                                      .toList();

        var included      = new java.util.ArrayList<String>();
        var excludedPairs = new java.util.ArrayList<String>();
        for (var n : allCompetition) {
            String driveA = n.properties().get("drive-a");
            String driveB = n.properties().get("drive-b");
            String text   = n.properties().get("differentiation-text");
            if (text == null || text.isBlank()) {continue;}
            if (activeDriveTypes.contains(driveA) && activeDriveTypes.contains(driveB)) {
                included.add(text);
            } else {
                excludedPairs.add(driveA + "↔" + driveB);
            }
        }

        if (!allCompetition.isEmpty()) {
            System.out.printf("[differentiation-filter] %s | total=%d | included=%d | excluded=%d [%s] | activeDrives=[%s]%n",
                              agentId, allCompetition.size(), included.size(), excludedPairs.size(),
                              String.join(", ", excludedPairs),
                              String.join(", ", activeDriveTypes));
        }

        return included;
    }

    private Map<String, String> readGeneratedDriveDescriptions() {
        if (mindMapStore == null || tenantId == null) return Map.of();
        var result = new java.util.HashMap<String, String>();
        mindMapStore.listSubgraphs(tenantId).stream()
            .filter(s -> "cognitive".equals(s.type()))
            .flatMap(s -> mindMapStore.nodesIn(s.id(), tenantId).stream())
            .filter(n -> n.traits().contains("DriveDescriptionGenerated"))
            .filter(n -> agentId.equals(n.properties().get("agent-id")))
            .forEach(n -> {
                String driveType = n.properties().get("drive-type");
                String desc = n.properties().get("generated-description");
                if (driveType != null && desc != null && !desc.isBlank()) {
                    result.put(driveType, desc);
                }
            });
        return result;
    }

    private Map<String, String> readSelfStandards(java.util.List<io.casehub.neocortex.mindmap.MindMapSubgraph> subgraphs) {
        var result = new java.util.HashMap<String, String>();
        subgraphs.stream()
            .filter(s -> "cognitive".equals(s.type()))
            .flatMap(s -> mindMapStore.nodesIn(s.id(), tenantId).stream())
            .filter(n -> n.traits().contains("SelfStandardDerived"))
            .filter(n -> agentId.equals(n.properties().get("agent-id")))
            .forEach(n -> {
                String driveType = n.properties().get("drive-type");
                String standard = n.properties().get("self-standard");
                if (driveType != null && standard != null && !standard.isBlank()) {
                    result.put(driveType, standard);
                }
            });
        return result;
    }

    List<String> selectSituationPrimedMemories(
            io.casehub.neocortex.caps.SituationContext situationContext,
            List<String> activatedDriveTypes) {
        if (mindMapStore == null || tenantId == null) {return List.of();}
        if (situationContext.isEmpty()) {return List.of();}
        if (activatedDriveTypes.isEmpty()) {return List.of();}

        var cognitiveNodes = mindMapStore.listSubgraphs(tenantId).stream()
                                         .filter(s -> "cognitive".equals(s.type()))
                                         .flatMap(s -> mindMapStore.nodesIn(s.id(), tenantId).stream())
                                         .toList();

        var profiles = cognitiveNodes.stream()
                                     .filter(n -> n.traits().contains("DriveProfileInferred"))
                                     .filter(n -> agentId.equals(n.properties().get("agent-id")))
                                     .filter(n -> activatedDriveTypes.contains(n.properties().get("drive-type")))
                                     .toList();

        record DriveOverlap(String driveType, double score) {}
        var overlaps = new java.util.ArrayList<DriveOverlap>();
        for (var profile : profiles) {
            String driveType = profile.properties().get("drive-type");
            String inputsStr = profile.properties().getOrDefault("inferred-inputs", "");
            double overlap   = computeInputOverlap(inputsStr, situationContext.confidenceMap());
            if (overlap > 0.0) {
                overlaps.add(new DriveOverlap(driveType, overlap));
            }
        }

        if (overlaps.isEmpty()) {return List.of();}
        overlaps.sort(java.util.Comparator.comparingDouble(DriveOverlap::score).reversed());

        var formativeMemories = cognitiveNodes.stream()
                                              .filter(n -> "formative-experience".equals(n.properties().get("cognitiveKind")))
                                              .filter(n -> agentId.equals(n.properties().get("agent-id")))
                                              .toList();

        var result = new java.util.ArrayList<String>();
        for (var o : overlaps) {
            if (result.size() >= 2) {break;}
            var memories = formativeMemories.stream()
                                            .filter(m -> {
                                                String types = m.properties().getOrDefault("situation-types", "");
                                                return java.util.Arrays.asList(types.split(",")).contains(o.driveType());
                                            })
                                            .limit(2)
                                            .map(m -> {
                                                String text = m.name().replaceAll("\\{\\{serves: [\\w-]+\\|\\w+: ", "").replace("}}", "");
                                                if (text.length() > 100) {
                                                    int end = text.lastIndexOf(' ', 100);
                                                    if (end < 40) {end = 100;}
                                                    text = text.substring(0, end) + "...";
                                                }
                                                return "A memory surfaces — " + text;
                                            })
                                            .toList();
            result.addAll(memories);
        }
        return result;
    }


    // Relative overlap threshold for CAPS profile discrimination (Mischel & Shoda 1995)
    // overlap/profileTotal must reach 0.15 to filter point-match from pattern-match
    private static final double OVERLAP_THRESHOLD = 0.15;
    // CAPS resting activation weight (Mischel & Shoda 1995, Higgins 1996 chronic accessibility)
    // effective_activation = situation_overlap + intensity² × RESTING_WEIGHT
    // At 0.22: drives ≥~0.85 self-sustain without situational overlap
    private static final double RESTING_WEIGHT = 0.22;

    // Relative overlap: overlap/profileTotal — Jaccard-family similarity metric
    private double computeInputOverlap(String inferredInputs, Map<String, Double> situationMap) {
        if (inferredInputs == null || inferredInputs.isBlank()) {return 0.0;}
        double overlap      = 0.0;
        double profileTotal = 0.0;
        for (String entry : inferredInputs.split(",")) {
            int colonIdx = entry.indexOf(':');
            if (colonIdx < 0) {continue;}
            String nodeId      = entry.substring(0, colonIdx).strip();
            double driveWeight = Double.parseDouble(entry.substring(colonIdx + 1).strip());
            profileTotal += driveWeight;
            Double situationWeight = situationMap.get(nodeId);
            if (situationWeight != null) {
                overlap += Math.min(driveWeight, situationWeight);
            }
        }
        if (profileTotal == 0.0) {return 0.0;}
        return overlap / profileTotal;
    }

    Map<String, String> buildSituationMetadata(Collection<String> nearbyAgentIds) {
        if (nearbyAgentIds == null || nearbyAgentIds.isEmpty()) return Map.of();
        var nearby = new java.util.StringJoiner(", ");
        for (var schema : socialConfig.relationalSchemas()) {
            if (nearbyAgentIds.contains(schema.target())) {
                nearby.add(schema.target() + " (" + schema.role() + ")");
            }
        }
        String nearbyStr = nearby.toString();
        return nearbyStr.isEmpty() ? Map.of() : Map.of("nearby-characters", nearbyStr);
    }

    List<SocialConfig.Drive> appraisalFilterDrives(
            List<SocialConfig.Drive> eligible,
            io.casehub.neocortex.caps.SituationContext situationContext,
            Collection<String> nearbyAgentIds) {
        if (mindMapStore == null || tenantId == null) {return eligible;}
        if (situationContext.isEmpty()) {
            if (situationClassifier != null) { lastClassifierActive = false; }
            return eligible;
        }
        lastClassifierActive = true;

        var profiles = readDriveProfiles();
        if (profiles.isEmpty()) {return eligible;}

        var included = new java.util.ArrayList<SocialConfig.Drive>();
        var excluded = new java.util.ArrayList<String>();
        for (var d : eligible) {
            String inputs = profiles.get(d.type());
            if (inputs == null) {
                included.add(d);
            } else {
                double overlap = computeInputOverlap(inputs, situationContext.confidenceMap());
                double resting = d.intensity() * d.intensity() * RESTING_WEIGHT;
                if (overlap + resting >= OVERLAP_THRESHOLD) {
                    included.add(d);
                } else {
                    excluded.add(d.type());
                }
            }
        }

        var activationStr = situationContext.activations().stream()
                                            .map(sa -> sa.nodeId() + ":" + String.format("%.2f", sa.confidence()))
                                            .collect(java.util.stream.Collectors.joining(", "));
        var includedStr = included.stream().map(SocialConfig.Drive::type)
                                  .collect(java.util.stream.Collectors.joining(", "));
        var ratioStr = eligible.stream()
                               .map(d -> {
                                   String inputs = profiles.get(d.type());
                                   if (inputs == null) {return d.type() + "=no-profile";}
                                   double overlap = computeInputOverlap(inputs, situationContext.confidenceMap());
                                   double resting = d.intensity() * d.intensity() * RESTING_WEIGHT;
                                   return d.type() + "=" + String.format("%.3f(overlap=%.3f,resting=%.3f)", overlap + resting, overlap, resting);
                               })
                               .collect(java.util.stream.Collectors.joining(", "));
        System.out.printf("[appraisal-filter] %s | situation=[%s] | ratios=[%s] | rendered=[%s] | filtered=[%s]%n",
                          agentId, activationStr, ratioStr, includedStr, String.join(", ", excluded));

        return included;
    }


    private Map<String, String> readDriveProfiles() {
        if (mindMapStore == null || tenantId == null) {return Map.of();}
        var result = new java.util.HashMap<String, String>();
        mindMapStore.listSubgraphs(tenantId).stream()
                    .filter(s -> "cognitive".equals(s.type()))
                    .flatMap(s -> mindMapStore.nodesIn(s.id(), tenantId).stream())
                    .filter(n -> n.traits().contains("DriveProfileInferred"))
                    .filter(n -> agentId.equals(n.properties().get("agent-id")))
                    .forEach(n -> {
                        String driveType = n.properties().get("drive-type");
                        String inputs    = n.properties().get("inferred-inputs");
                        if (driveType != null && inputs != null) {
                            result.put(driveType, inputs);
                        }
                    });
        return result;
    }


    private ObservationSection renderRelationalSchemas(Collection<String> nearbyAgentIds, Map<String, String> agentNames) {
        var relevant = socialConfig.relationalSchemas().stream()
                                   .filter(rs -> nearbyAgentIds.contains(rs.target()))
                                   .toList();
        if (relevant.isEmpty()) {return null;}

        var sb = new StringBuilder();
        for (var rs : relevant) {
            var displayName = agentNames.getOrDefault(rs.target(), rs.target());
            sb.append("**").append(displayName).append(":** ");
            if (!rs.description().isEmpty()) {
                sb.append(rs.description());
            }
            sb.append("\n");
        }
        return ObservationSection.text("How You Feel About Those Present", sb.toString().strip());
    }

    // Attachment quadrants — Bartholomew & Horowitz (1991) anxiety × avoidance two-dimensional model
    private ObservationSection renderAttachmentStyle() {
        var a  = socialConfig.attachment();
        var sb = new StringBuilder();
        if (a.avoidance() > 0.6) {
            sb.append("You don't need people. Not really. When someone tries to get close, ");
            sb.append("you feel faint irritation. Closeness is a vulnerability you learned to avoid.");
        } else if (a.anxiety() > 0.5) {
            sb.append("You worry about the people you love. When they're out of sight, ");
            sb.append("something in you tightens. You need to know they're safe.");
        } else if (a.avoidance() < 0.3 && a.anxiety() < 0.3) {
            sb.append("You trust easily and feel comfortable with closeness. ");
            sb.append("When someone you care about is near, you feel settled.");
        } else {
            sb.append("You are cautious with closeness but capable of trust once earned.");
        }
        return ObservationSection.text("How You Bond", sb.toString());
    }


    private static ObservationSection adaptPromptSection(String text) {
        if (text.startsWith("## ")) {
            int end = text.indexOf('\n');
            if (end > 0) {
                return ObservationSection.text(text.substring(3, end).strip(), text.substring(end).strip());
            }
        }
        int nl = text.indexOf('\n');
        if (nl > 0) {
            return ObservationSection.text(text.substring(0, nl).strip(), text.substring(nl).strip());
        }
        return ObservationSection.text("Cognitive State", text);
    }


    private java.util.List<ObservationSection> renderTrustSections() {
        if (mindMapStore == null || trustEvolutionConfig == null || tenantId == null) return List.of();

        var summaries = new java.util.ArrayList<io.casehub.agency.perception.observation.affordance.TrustSummary>();

        var subgraphs = mindMapStore.listSubgraphs(tenantId);
        var peopleSubgraph = subgraphs.stream()
            .filter(sg -> "people".equals(sg.name()))
            .findFirst();
        if (peopleSubgraph.isEmpty()) return List.of();

        var nodes = mindMapStore.nodesIn(peopleSubgraph.get().id(), tenantId);
        var sharedNodes = new java.util.HashMap<String, io.casehub.neocortex.mindmap.MindMapNode>();
        for (var node : nodes) {
            if (!node.traits().contains("overlay")) {
                node.property("agentId").ifPresent(aid -> sharedNodes.put(node.id(), node));
            }
        }

        for (var overlay : nodes) {
            if (!overlay.traits().contains("overlay")) continue;
            if (!agentId.equals(overlay.property(io.casehub.neocortex.mindmap.OverlayRef.AGENT_ID).orElse(null))) continue;

            var trustScoreStr = overlay.property(io.casehub.engine.trust.OverlayTrustPropertyModel.TRUST_SCORE);
            var alphaStr = overlay.property(io.casehub.engine.trust.OverlayTrustPropertyModel.TRUST_ALPHA);
            var betaStr = overlay.property(io.casehub.engine.trust.OverlayTrustPropertyModel.TRUST_BETA);
            if (trustScoreStr.isEmpty()) continue;

            double score = Double.parseDouble(trustScoreStr.get());
            double alpha = alphaStr.map(Double::parseDouble).orElse(1.0);
            double beta = betaStr.map(Double::parseDouble).orElse(1.0);

            // Uninformative Beta prior — alpha=beta=1, sum≤2 means no evidence (Jøsang 2016, Subjective Logic)
            if (alpha + beta <= 2.0) continue;

            String targetNodeId = io.casehub.neocortex.mindmap.OverlayRef.sharedNodeId(overlay).orElse(null);
            if (targetNodeId == null) continue;

            var sharedNode = sharedNodes.get(targetNodeId);
            if (sharedNode == null) continue;
            String subjectName = sharedNode.name();

            var levels = trustEvolutionConfig.levels();
            io.casehub.agency.perception.observation.affordance.TrustLevel level;
            if (score >= levels.high()) level = io.casehub.agency.perception.observation.affordance.TrustLevel.HIGH;
            else if (score >= levels.moderate()) level = io.casehub.agency.perception.observation.affordance.TrustLevel.MODERATE;
            else level = io.casehub.agency.perception.observation.affordance.TrustLevel.LOW;

            // Beta posterior concentration (Kruschke 2015) — sum>10 narrow posterior, <4 wide
            String reason = (alpha + beta > 10) ? "You feel quite certain about this"
                          : (alpha + beta < 4) ? "You're still forming an opinion"
                          : null;

            summaries.add(new io.casehub.agency.perception.observation.affordance.TrustSummary(subjectName, level, reason));
        }

        if (summaries.isEmpty()) return List.of();
        return List.of(io.casehub.agency.perception.observation.affordance.CognitiveObservationSections.trustSection(summaries));
    }


    java.util.List<SocialConfig.Drive> resolveAdaptedDrives() {
        if (mindMapStore == null || tenantId == null) {
            return socialConfig.drives();
        }
        var subgraphs = mindMapStore.listSubgraphs(tenantId);
        var driveNodes = subgraphs.stream()
                                  .filter(s -> "cognitive".equals(s.type()))
                                  .flatMap(s -> mindMapStore.nodesIn(s.id(), tenantId).stream())
                                  .filter(n -> "drive-intensity".equals(n.properties().get("cognitiveKind")))
                                  .filter(n -> agentId.equals(n.properties().get("agent-id")))
                                  .toList();
        if (driveNodes.isEmpty()) {
            return socialConfig.drives();
        }
        var contextByType = socialConfig.drives().stream()
                                        .collect(java.util.stream.Collectors.toMap(SocialConfig.Drive::type, SocialConfig.Drive::context, (a, b) -> a));
        return driveNodes.stream()
                         .map(n -> {
                             String driveType = n.properties().get("drive-type");
                             return new SocialConfig.Drive(
                                     driveType,
                                     Double.parseDouble(n.properties().getOrDefault("intensity", "0")),
                                     n.properties().getOrDefault("description", ""),
                                     contextByType.getOrDefault(driveType, "any"));
                         })
                         .toList();
    }


    String behavioralCue(String stage) {
        boolean cooperate = contextStrategy.shouldCooperate(stage);
        boolean disclose  = contextStrategy.shouldDisclose(stage);
        if (!cooperate) {return null;}
        if (disclose) {return "Willing to cooperate and share openly";}
        return "Willing to cooperate";
    }

    List<ObservationSection> renderSocialAwareness(
            java.util.Collection<String> nearbyAgentIds,
            java.util.Map<String, String> agentNames) {
        if (cognitiveProfile == null || tenantId == null) {
            return List.of();
        }
        if (!contextStrategy.shouldCompareSocially(resolveAdaptedDrives(), false)) {
            return List.of();
        }

        var stageMap = new java.util.HashMap<String, String>();
        if (mindMapStore != null) {
            var subgraphs = mindMapStore.listSubgraphs(tenantId);
            var peopleSg = subgraphs.stream()
                                    .filter(sg -> "people".equals(sg.name())).findFirst();
            if (peopleSg.isPresent()) {
                var overlayNodes = mindMapStore.nodesIn(peopleSg.get().id(), tenantId);
                var sharedNodes  = new java.util.HashMap<String, String>();
                for (var node : overlayNodes) {
                    if (!node.traits().contains("overlay")) {
                        node.property("agentId").ifPresent(aid -> sharedNodes.put(node.id(), aid));
                    }
                }
                for (var node : overlayNodes) {
                    if (!node.traits().contains("overlay")) {continue;}
                    if (!agentId.equals(node.property(io.casehub.neocortex.mindmap.OverlayRef.AGENT_ID).orElse(null))) {
                        continue;
                    }
                    var targetNodeId = io.casehub.neocortex.mindmap.OverlayRef.sharedNodeId(node).orElse(null);
                    if (targetNodeId == null) {continue;}
                    var targetId = sharedNodes.get(targetNodeId);
                    if (targetId == null) {continue;}
                    var stage = node.property(io.casehub.neocortex.cognition.relationship.OverlayFamiliarityPropertyModel.FAMILIARITY_STAGE).orElse("stranger");
                    stageMap.put(targetId, stage);
                }
            }
        }

        var selfPrincipal = io.casehub.platform.api.identity.PrincipalId.agent(agentId);
        var lines         = new java.util.ArrayList<String>();

        for (String nearbyId : nearbyAgentIds) {
            var    otherPrincipal = io.casehub.platform.api.identity.PrincipalId.agent(nearbyId);
            String otherName      = agentNames.getOrDefault(nearbyId, nearbyId);
            String stage          = stageMap.getOrDefault(nearbyId, "stranger");

            String perceptionLine = null;
            try {
                var query = io.casehub.neocortex.cognitive.index.CognitiveProfileQuery
                                    .byName(nearbyId, tenantId);
                var perspectives = cognitiveProfile.compare(query,
                                                            java.util.Set.of(selfPrincipal, otherPrincipal));
                if (!perspectives.isEmpty()) {
                    var comparison = io.casehub.neocortex.cognitive.index.SocialComparison
                                             .compare(perspectives);
                    perceptionLine = PerceptionTranslator.translate(comparison, selfPrincipal, otherPrincipal, otherName, stage)
                                                         .orElse(null);
                }
            } catch (Exception e) {
                // graceful degradation
            }

            String behavioralCue = behavioralCue(stage);
            if (perceptionLine != null && behavioralCue != null) {
                lines.add(perceptionLine + ". " + behavioralCue);
            } else if (perceptionLine != null) {
                lines.add(perceptionLine);
            } else if (behavioralCue != null) {
                lines.add(otherName + " (" + stage + "): " + behavioralCue);
            }
        }

        if (lines.isEmpty()) {return List.of();}
        return List.of(ObservationSection.items("Social Awareness", null, lines));
    }

}

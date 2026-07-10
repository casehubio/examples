# CaseHub Eidos — Examples

Executable examples demonstrating eidos capabilities. Each example is a `@QuarkusTest`
that runs with in-memory stores (zero datasource) and well-known vocabularies.

## Running

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl examples/agent-scenarios --also-make
```

## Capability Coverage

| Capability | Example | Status |
|---|---|---|
| AgentDescriptor creation | MultiAgentTeamTest | ✅ |
| AgentRegistry.register | MultiAgentTeamTest | ✅ |
| AgentRegistry.findById | MultiAgentTeamTest, TenancyIsolationTest | ✅ |
| AgentQuery.bySlot | MultiAgentTeamTest | ✅ |
| AgentQuery.byCapability | MultiAgentTeamTest | ✅ |
| AgentQuery.bySlotAndCapability | MultiAgentTeamTest | ✅ |
| AgentQuery.all | TenancyIsolationTest | ✅ |
| Tenancy isolation | TenancyIsolationTest | ✅ |
| VocabularyRegistry.find | CrossVocabularyDiscoveryTest | ✅ |
| VocabularyRegistry.resolve | CrossVocabularyDiscoveryTest, DispositionVocabularyTest | ✅ |
| VocabularyRegistry.equivalentValues | CrossVocabularyDiscoveryTest | ✅ |
| SVO vocabulary | CrossVocabularyDiscoveryTest | ✅ |
| CasehubSlot vocabulary | CrossVocabularyDiscoveryTest | ✅ |
| Conscientiousness vocabulary (12 terms, 4 axes) | DispositionVocabularyTest | ✅ |
| CapabilityHealth.probe → Ready | EpistemicDomainMatchingTest | ✅ |
| CapabilityHealth.probe → Unavailable | EpistemicDomainMatchingTest | ✅ |
| CapabilityHealth.probe → EpistemicallyWeak | EpistemicDomainMatchingTest | ✅ |
| CapabilityHealth.probe → Degraded | — | Phase 3 (runtime state infrastructure) |
| ReactiveCapabilityHealth | — | Implemented, build-gated |
| SystemPromptRenderer | — | Phase 3 ([#5](https://github.com/casehubio/eidos/issues/5)) |
| Knowledge graph | — | Phase 4 |

## Module Index

| Module | Focus | Dependencies |
|---|---|---|
| `agent-scenarios` | Core identity, discovery, vocabulary, health probing | eidos + eidos-memory + eidos-vocab |

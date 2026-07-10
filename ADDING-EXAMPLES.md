# Adding a New Repo's Examples

LLM-executable checklist for onboarding a new repo's examples into
casehub-examples. Follow these steps in order.

## Prerequisites (in the source repo)

1. **Verify `examples/` directory exists** with at least one runnable
   example (Maven module, Docker Compose, or TypeScript project).

2. **Verify POM parent resolution.** Example POMs that inherit from a
   parent (e.g., `<parent>casehub-work-parent</parent>`) must set
   `<relativePath/>` (empty, self-closing) in the `<parent>` block.
   This tells Maven to resolve from the repository, not the filesystem.
   Without it, the examples won't build after subtree extraction.

   Standalone POMs (like ledger's — no `<parent>`) need no changes.

3. **Verify aggregator POM.** The source repo must have a
   `examples/pom.xml` with `<packaging>pom</packaging>` that lists
   all example subdirectories as `<modules>`. If absent, create one:

   ```xml
   <?xml version="1.0"?>
   <project xmlns="http://maven.apache.org/POM/4.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
     <modelVersion>4.0.0</modelVersion>

     <groupId>io.casehub.<repo>.examples</groupId>
     <artifactId>casehub-<repo>-examples</artifactId>
     <version>0.2-SNAPSHOT</version>
     <packaging>pom</packaging>
     <name>CaseHub <Repo> - Examples</name>

     <modules>
       <module>example-one</module>
       <module>example-two</module>
     </modules>
   </project>
   ```

   This ensures new examples added to the source repo are
   automatically included in the aggregator build.

## In casehub-examples

4. **Add to `sync-config.json`:**

   ```json
   {"name": "<repo>", "org": "casehubio", "type": "maven"}
   ```

   Use `type`: `maven`, `docker`, or `typescript`.

5. **Run the sync locally:**

   ```bash
   ./sync.sh
   ```

   This pulls the examples via subtree-split. Verify the
   `<repo>-examples/` directory appears with the expected content.

6. **If Maven — update `pom.xml`:**

   Add a single `<module>` entry:

   ```xml
   <module><repo>-examples</module>
   ```

   The source repo's own aggregator POM handles internal modules.

7. **Verify the build:**

   ```bash
   mvn test
   ```

   All examples must pass. Fix any dependency resolution issues
   (usually a missing `<relativePath/>` in the source repo).

8. **Update `README.md`:**

   Add a row to the "Example Sets" table and a source-repo mapping
   to the "About This Repo" section.

9. **Commit and push:**

   The automated sync keeps it current from here.

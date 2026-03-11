# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IntelliJ IDEA plugin that displays `@Schema(description)` and `@Parameter(description)` from Swagger/OpenAPI annotations in the Quick Documentation popup (Ctrl+Q). Written in Java 21, built with Gradle 9.4 and the IntelliJ Platform Gradle Plugin 2.3.0.

- **Plugin ID:** `com.livalife.schema-doc`
- **Target IDE:** IntelliJ IDEA Community 2024.3+ (build 243+)
- **Dependency:** `com.intellij.modules.java` (bundled Java plugin)

## Build Commands

```bash
./gradlew build          # Compile + package the plugin
./gradlew runIde         # Launch a sandbox IDE with the plugin installed
./gradlew buildPlugin    # Build distributable plugin ZIP
./gradlew verifyPlugin   # Run IntelliJ plugin verifier
```

## Architecture

Single-class plugin — the entire logic lives in `SchemaDocumentationProvider` (`src/main/java/com/livalife/schemadoc/`), registered as a `lang.documentationProvider` extension in `plugin.xml`.

**Flow:** `generateDoc()` is called by the IDE's documentation system. It checks if the PSI element has `@Schema` (FQN: `io.swagger.v3.oas.annotations.media.Schema`) or `@Parameter` (FQN: `io.swagger.v3.oas.annotations.Parameter`) annotations, resolves the `description` (and optionally `example`) attribute values (supporting both literals and compile-time constants), and renders HTML sections appended to the default Javadoc. For getter/setter/is-methods, it also looks up `@Schema` on the backing field.

**Key design decisions:**
- Registered with `order="first"` so it runs before the default Java documentation provider
- Delegates to `JavaDocumentationProvider` to get base Javadoc, then appends annotation sections; falls back to a standalone rendering when no Javadoc exists
- Returns `null` (falls through to default provider) when no supported annotations are present
- Uses `JavaPsiFacade.getConstantEvaluationHelper()` to resolve constant expressions in annotation attributes

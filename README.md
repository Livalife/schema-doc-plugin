# Schema Documentation Provider

IntelliJ IDEA plugin that displays `@Schema(description)` and `@Parameter(description)` from Swagger/OpenAPI annotations in the Quick Documentation popup (Ctrl+Q).

When a Java element is annotated with `@Schema(description = "...")` or `@Parameter(description = "...")`, hovering or pressing Ctrl+Q shows the annotation description in the Quick Documentation popup. If Javadoc already exists, the annotation information is appended below it.

## Features

- Shows `@Schema(description)` and `@Parameter(description)` in the Quick Documentation popup
- Displays `example` attribute when present on either annotation
- Appends to existing Javadoc instead of replacing it
- Resolves `@Schema` from backing fields when hovering over getter/setter/is-methods
- Resolves compile-time constant expressions in annotation attributes
- Supports classes, fields, methods, and parameters

## Example

```java
@Schema(description = "User's email address", example = "john@example.com")
private String email;
```

Pressing Ctrl+Q on `email` will display:

> **@Schema:** User's email address
>
> **Example:** `john@example.com`

## Requirements

- IntelliJ IDEA 2024.3+ (build 243+)
- Java 21+

## Building

```bash
./gradlew build          # Compile + package the plugin
./gradlew runIde         # Launch a sandbox IDE with the plugin installed
./gradlew buildPlugin    # Build distributable plugin ZIP
./gradlew verifyPlugin   # Run IntelliJ plugin verifier
```

## Installation

1. Run `./gradlew buildPlugin`
2. In IntelliJ IDEA, go to **Settings > Plugins > Install Plugin from Disk...**
3. Select the ZIP file from `build/distributions/`

## Tech Stack

- Java 21
- Gradle 9.4
- IntelliJ Platform Gradle Plugin 2.3.0

# How to Build and Run Jubler

## Prerequisites

- Java 8 or higher
- Gradle (use the system `gradle`)

## Build & run

```bash
gradle build                 # compile all modules, run tests, produce jars
gradle assembleDistribution  # full runnable distribution in build/jubler/
java -jar build/jubler/lib/jubler.jar
```

With arguments:

```bash
java -jar build/jubler/lib/jubler.jar --help
java -jar build/jubler/lib/jubler.jar --load subtitle.srt
java -jar build/jubler/lib/jubler.jar --convert input.ass output.srt
```

## Common tasks

```bash
gradle clean build           # clean rebuild
gradle build -x test         # skip tests
gradle test                  # tests only
gradle :core:build           # build a single module
gradle :core:compileJava     # quick compile check while developing
```

## Distribution layout

`gradle assembleDistribution` produces `build/jubler/`:

- `lib/jubler.jar` — launcher (main entry point), plus the module and dependency jars
- `lib/i18n/` — translations
- `README.md`, `LICENCE.txt`

## Versioning

The version is derived automatically from the latest `v*` git tag (e.g. tag `v10.0.0` builds
version `10.0.0`); there is no manual version-bump command. See `make.sh` for the available helpers
(`build`, `winget`, `clean`, `headers`).

# How to Build and Run Jubler

This guide explains how to compile, build, and run Jubler using Gradle.

## Prerequisites

- Java 8 or higher
- Gradle installed on your system (or use `./gradlew` if wrapper is configured)

## Quick Start

### 1. Compile Everything
```bash
gradle build
```
This compiles all modules, runs tests, and creates JAR files.

### 2. Create a Complete Distribution
```bash
gradle assembleDistribution
```
This creates a runnable distribution in `build/jubler/` with all dependencies.

### 3. Run Jubler
```bash
java -jar build/jubler/lib/jubler.jar
```

Or with arguments:
```bash
java -jar build/jubler/lib/jubler.jar --help
java -jar build/jubler/lib/jubler.jar --load subtitle.srt
java -jar build/jubler/lib/jubler.jar --convert input.ass output.srt
```

## Common Build Tasks

### Clean Build
Remove all previous build artifacts:
```bash
gradle clean
```

### Clean and Rebuild
```bash
gradle clean build
```

### Build Without Tests
```bash
gradle build -x test
```

### Run Tests Only
```bash
gradle test
```

### Build Specific Module
```bash
gradle :core:build
gradle :launcher:jar
```

## Development Workflow

### Typical Development Cycle

1. **Make code changes** in any module (e.g., `modules/core/src/main/java/...`)

2. **Quick compile check:**
   ```bash
   gradle :core:compileJava
   ```

3. **Build changed module:**
   ```bash
   gradle :core:build
   ```

4. **Build everything and create distribution:**
   ```bash
   gradle clean assembleDistribution
   ```

5. **Test your changes:**
   ```bash
   java -jar build/jubler/lib/jubler.jar
   ```

### Incremental Builds

Gradle is smart about incremental builds. If you only changed one module:
```bash
# Only rebuilds what changed
gradle build
```

Gradle automatically detects:
- Which modules changed
- Which modules depend on changed modules
- Which tasks can be skipped

## Distribution Structure

After running `gradle assembleDistribution`, you'll find:

```
build/jubler/
├── lib/
│   ├── jubler.jar           # Launcher (main entry point)
│   ├── core.jar             # Core functionality
│   ├── aspell.jar           # Spell checking plugin
│   ├── autoupdate.jar       # Auto-update plugin
│   ├── azuretranslate.jar   # Azure translation plugin
│   ├── basetextsubs.jar     # Base subtitle formats
│   ├── coretools.jar        # Editing tools
│   ├── coretheme.jar        # UI themes
│   ├── mplayer.jar          # MPlayer integration
│   ├── vlcj-jubler.jar      # VLC integration
│   ├── zemberek.jar         # Turkish spell checking
│   ├── [external dependencies...]
│   ├── i18n/                # Translation files
│   └── help/                # Help resources
├── README.md
└── LICENCE.txt
```

## Module-Specific Builds

### Build Individual Modules

```bash
# Launcher (entry point)
gradle :launcher:build

# Core module
gradle :core:build

# All plugin modules
gradle :aspell:build :autoupdate:build :vlcj-jubler:build
```

### View Module Dependencies

```bash
# See all dependencies for a module
gradle :core:dependencies

# See runtime dependencies only
gradle :core:dependencies --configuration runtimeClasspath
```

## Troubleshooting

### Build Fails with "Cannot find symbol"

Dependencies might not be resolved. Try:
```bash
gradle clean build --refresh-dependencies
```

### "Out of memory" Errors

Increase Gradle memory by creating/editing `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2g
```

### Gradle Daemon Issues

If Gradle behaves strangely, kill the daemon:
```bash
gradle --stop
```

Then rebuild:
```bash
gradle clean build
```

### Force Full Rebuild

```bash
gradle clean build --no-build-cache
```

## Performance Tips

### Use Gradle Daemon (Faster Builds)

The daemon keeps Gradle running in the background:
```bash
# Let daemon start automatically (default)
gradle build

# Or explicitly enable it
echo "org.gradle.daemon=true" >> gradle.properties
```

### Parallel Builds

Enable parallel execution in `gradle.properties`:
```properties
org.gradle.parallel=true
```

### Skip Unnecessary Tasks

```bash
# Skip tests
gradle build -x test

# Skip specific module
gradle build -x :zemberek:build
```

## Build Output Locations

- **Compiled classes:** `modules/*/build/classes/java/main/`
- **JAR files:** `modules/*/build/libs/*.jar`
- **Test results:** `modules/*/build/reports/tests/test/`
- **Distribution:** `build/jubler/`

## Quick Reference

| Task | Command |
|------|---------|
| Compile everything | `gradle build` |
| Create distribution | `gradle assembleDistribution` |
| Run application | `java -jar build/jubler/lib/jubler.jar` |
| Clean build | `gradle clean build` |
| Run tests | `gradle test` |
| Build without tests | `gradle build -x test` |
| Build specific module | `gradle :core:build` |
| List all tasks | `gradle tasks --all` |
| Get help on task | `gradle help --task build` |

## Comparison with Maven

| Maven Command | Gradle Equivalent |
|---------------|-------------------|
| `mvn clean` | `gradle clean` |
| `mvn compile` | `gradle classes` |
| `mvn test` | `gradle test` |
| `mvn package` | `gradle jar` |
| `mvn install` | `gradle build` |
| `mvn clean install` | `gradle clean build` |
| `mvn clean install -DskipTests` | `gradle clean build -x test` |
| `mvn clean install -P generic` | `gradle clean assembleDistribution` |

## Environment Variables

You can override build properties:
```bash
# Use specific Java version
JAVA_HOME=/path/to/java8 gradle build

# Increase memory
GRADLE_OPTS="-Xmx2g" gradle build
```

## IDE Integration

### IntelliJ IDEA
1. Open project
2. IDEA auto-detects Gradle
3. Click "Load Gradle Project"
4. Build → Build Project

### Eclipse
1. Install Buildship plugin
2. Import → Gradle → Existing Gradle Project
3. Select project root

### VS Code
1. Install "Gradle for Java" extension
2. Open project
3. Use Gradle tasks view

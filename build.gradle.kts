import java.util.concurrent.TimeUnit

plugins {
    java
}

group = "com.panayotis.jubler"

// The version comes from an explicit `-Pversion=` override, otherwise from the latest git tag.
// It is resolved lazily (and memoised) the first time the version string is actually needed —
// tasks that never stamp a version (e.g. `gradle tasks`) never shell out to git. A missing tag or
// a shallow clone degrades to a SNAPSHOT instead of failing the build.
val explicitVersion = (project.findProperty("version") as String?)
    ?.takeIf { it.isNotEmpty() && it != "unspecified" }
version = explicitVersion ?: object {
    private val resolved by lazy { versionFromGit() }
    override fun toString() = resolved
}

allprojects {
    repositories {
        mavenCentral()
    }
}

// Distribution assembly.
// A single resolvable configuration aggregates every module: each project dependency contributes
// its own jar plus its transitive runtime dependencies, so resolving it yields the complete set of
// module jars + external jars (deduplicated by Gradle) that make up the runtime distribution.
val distributionRuntime = configurations.create("distributionRuntime") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    subprojects.forEach { module ->
        add(distributionRuntime.name, project(module.path))
    }
}

// Strips the trailing version from a jar file name (e.g. `minimal-json-0.9.5.jar` -> `minimal-json.jar`).
val stripVersion = Regex("-\\d+(\\.\\d+)*(-[A-Za-z0-9]+)?\\.jar")

// Collect all runtime jars (version-stripped) and the translations into build/jubler/lib.
val copyLibraries = tasks.register<Sync>("copyLibraries") {
    group = "distribution"
    description = "Collects all module and dependency jars plus translations into the distribution lib directory"

    from(distributionRuntime) {
        rename { it.replace(stripVersion, ".jar") }
    }
    from("resources/i18n") {
        include("*.json")
        into("i18n")
    }
    into(layout.buildDirectory.dir("jubler/lib"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Top-level distribution docs.
val copyResources = tasks.register<Copy>("copyResources") {
    group = "distribution"
    description = "Copies the top-level distribution documents"

    from(".") {
        include("README.md")
    }
    from(".") {
        include("LICENSE")
        rename { "LICENCE.txt" }
    }
    into(layout.buildDirectory.dir("jubler"))
}

// Assemble each platform's distribution directory. One Copy per platform, each scoped to its own
// `build/jubler-<platform>` destination so no task declares the whole `build/` dir as its output —
// that overlap used to trip Gradle's implicit-dependency validation against `test`/`jar`/etc.
val platformTargets = listOf(
    "jubler-linux64", "jubler-linuxarm64", "jubler-macos", "jubler-macosarm64", "jubler-win64", "jubler-generic"
)

val platformCopyTasks = platformTargets.map { target ->
    tasks.register<Copy>("copyPlatform-$target") {
        group = "distribution"
        description = "Assembles the $target distribution directory"

        from(copyLibraries) {
            into("lib")
        }
        from(copyResources)

        into(layout.buildDirectory.dir(target))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks.register("copyPlatformExtras") {
    group = "distribution"
    description = "Assembles every platform-specific distribution directory"
    dependsOn(platformCopyTasks)
}

tasks.register("assembleDistribution") {
    group = "distribution"
    description = "Assembles the complete Jubler distribution"

    dependsOn(copyLibraries, copyResources, platformCopyTasks)
}

fun versionFromGit(): String {
    val fallback = "0.0.0-SNAPSHOT"
    return try {
        val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
            .directory(rootProject.projectDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroy()
            fallback
        } else if (process.exitValue() == 0) {
            val gitTag = process.inputStream.bufferedReader().readText().trim()
            gitTag.removePrefix("v").removePrefix("V").ifEmpty { fallback }
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}
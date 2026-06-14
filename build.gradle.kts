import java.util.concurrent.TimeUnit

plugins {
    java
}

group = "com.panayotis.jubler"
version = (project.findProperty("version") as String?)
    ?.takeIf { it.isNotEmpty() && it != "unspecified" }
    ?: getVersionFromGit()

val longVersion by extra("7.0.1.0")
val releaseVersion by extra("1325")
val jsonVersion by extra("0.9.5")

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial"))
    }

    group = rootProject.group
    version = rootProject.version
}

// Distribution tasks (migrated from installer module)
// Collect all runtime dependencies from all modules
val allRuntimeDependencies by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

tasks.register<Copy>("copyDependencies") {
    group = "distribution"
    description = "Copies all dependencies to distribution directory"

    // Exclude i18n-tools - it's a build-time utility, not part of the runtime distribution
    val distributionProjects = subprojects.filter { it.name != "i18n-tools" }
    dependsOn(distributionProjects.map { it.tasks.named("jar") })

    // Collect runtime classpath from all modules (excluding i18n-tools)
    distributionProjects.forEach { subproject ->
        from(subproject.configurations.getByName("runtimeClasspath"))
    }
    into(layout.buildDirectory.dir("jubler/lib"))

    exclude("**/jupidator-project-*.jar", "**/project-*.jar")

    // Remove version from jar names (stripVersion equivalent)
    rename { filename ->
        filename.replace(Regex("-\\d+(\\.\\d+)*(-ALPHA|-BETA|-SNAPSHOT)?\\.jar"), ".jar")
    }

    // Avoid duplicates
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Copy>("copyModuleJars") {
    group = "distribution"
    description = "Copies all module JARs to distribution directory"

    // Exclude i18n-tools - it's a build-time utility, not part of the runtime distribution
    val distributionProjects = subprojects.filter { it.name != "i18n-tools" }
    dependsOn(distributionProjects.map { it.tasks.named("jar") })

    distributionProjects.forEach { subproject ->
        from(subproject.tasks.named("jar"))
    }

    into(layout.buildDirectory.dir("jubler/lib"))

    // Remove version from jar names
    rename { filename ->
        filename.replace(Regex("-\\d+(\\.\\d+)*(-ALPHA|-BETA|-SNAPSHOT)?\\.jar"), ".jar")
    }
}

tasks.register<Copy>("copyResources") {
    group = "distribution"
    description = "Copies resources to distribution directory"

    from("resources/i18n") {
        include("*.json")
        into("lib/i18n")
    }
    from(".") {
        include("README.md")
    }
    from(".") {
        include("LICENSE")
        rename { "LICENCE.txt" }
    }

    into(layout.buildDirectory.dir("jubler"))
}

tasks.register<Copy>("copyPlatformExtras") {
    group = "distribution"
    description = "Copies platform-specific native libraries to distribution directories"

    dependsOn("copyDependencies", "copyModuleJars", "copyResources")

    from("resources/installer/extra/linux64/lib") {
        into("jubler-linux64/lib/lib")
    }
    from("resources/installer/extra/macos/lib") {
        into("jubler-macos/lib/lib")
    }
    from("resources/installer/extra/win64/lib") {
        into("jubler-win64/lib/lib")
    }

    from(layout.buildDirectory.dir("jubler/lib")) {
        into("jubler-linux64/lib")
    }
    from(layout.buildDirectory.dir("jubler/lib")) {
        into("jubler-linuxarm64/lib")
    }
    from(layout.buildDirectory.dir("jubler/lib")) {
        into("jubler-macos/lib")
    }
    from(layout.buildDirectory.dir("jubler/lib")) {
        into("jubler-macosarm64/lib")
    }
    from(layout.buildDirectory.dir("jubler/lib")) {
        into("jubler-win64/lib")
    }
    from(layout.buildDirectory.dir("jubler/lib")) {
        into("jubler-generic/lib")
    }

    from(layout.buildDirectory.file("jubler/README.md")) {
        into("jubler-linux64")
    }
    from(layout.buildDirectory.file("jubler/LICENCE.txt")) {
        into("jubler-linux64")
    }
    from(layout.buildDirectory.file("jubler/README.md")) {
        into("jubler-linuxarm64")
    }
    from(layout.buildDirectory.file("jubler/LICENCE.txt")) {
        into("jubler-linuxarm64")
    }
    from(layout.buildDirectory.file("jubler/README.md")) {
        into("jubler-macos")
    }
    from(layout.buildDirectory.file("jubler/LICENCE.txt")) {
        into("jubler-macos")
    }
    from(layout.buildDirectory.file("jubler/README.md")) {
        into("jubler-macosarm64")
    }
    from(layout.buildDirectory.file("jubler/LICENCE.txt")) {
        into("jubler-macosarm64")
    }
    from(layout.buildDirectory.file("jubler/README.md")) {
        into("jubler-win64")
    }
    from(layout.buildDirectory.file("jubler/LICENCE.txt")) {
        into("jubler-win64")
    }
    from(layout.buildDirectory.file("jubler/README.md")) {
        into("jubler-generic")
    }
    from(layout.buildDirectory.file("jubler/LICENCE.txt")) {
        into("jubler-generic")
    }

    into(layout.buildDirectory)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register("assembleDistribution") {
    group = "distribution"
    description = "Assembles the complete Jubler distribution (equivalent to 'mvn clean install -P generic -DskipTests')"

    dependsOn("copyDependencies", "copyModuleJars", "copyResources", "copyPlatformExtras")
}

// The root project has no sources, so its `jar` task produces an empty archive in
// build/libs. copyPlatformExtras writes into the whole build/ directory, which overlaps
// that location. Declare the ordering so Gradle 9's strict validation doesn't fail when
// `build` and `assembleDistribution` are requested in the same invocation.
tasks.named("jar") {
    mustRunAfter("copyPlatformExtras")
}

// Platform-specific distribution profiles
val macDist: String by project.extra { "" }
val win32Dist: String by project.extra { "" }
val win64Dist: String by project.extra { "" }
val linuxDist: String by project.extra { "" }
val genericDist: String by project.extra { "" }
val arm32Dist: String by project.extra { "" }
val arm64Dist: String by project.extra { "" }

val makeappExt = if (System.getProperty("os.name").lowercase().contains("mac")) "mac" else "linux"

fun getVersionFromGit(): String {
    val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
        .directory(rootProject.projectDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    process.waitFor(5, TimeUnit.SECONDS)

    if (process.exitValue() == 0) {
        val gitTag = process.inputStream.bufferedReader().readText().trim()
        return gitTag.removePrefix("v").removePrefix("V")
    } else {
        val error = process.errorStream.bufferedReader().readText()
        throw GradleException("Failed to get version from git tag: $error")
    }
}
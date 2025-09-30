plugins {
    java
}

group = "com.panayotis.jubler"
version = "9.0.0-ALPHA"

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

    dependsOn(subprojects.map { it.tasks.named("jar") })

    // Collect runtime classpath from all modules
    subprojects.forEach { subproject ->
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

    dependsOn(subprojects.map { it.tasks.named("jar") })

    subprojects.forEach { subproject ->
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
    from("resources/help/cache") {
        include("jubler-faq.html")
        into("lib/help")
    }
    from("resources/help") {
        include("question.png")
        into("lib/help")
    }
    from(".") {
        include("README.md")
    }
    from(".") {
        include("LICENCE")
        rename { "LICENCE.txt" }
    }

    into(layout.buildDirectory.dir("jubler"))
}

tasks.register("assembleDistribution") {
    group = "distribution"
    description = "Assembles the complete Jubler distribution (equivalent to 'mvn clean install -P generic -DskipTests')"

    dependsOn("copyDependencies", "copyModuleJars", "copyResources")
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

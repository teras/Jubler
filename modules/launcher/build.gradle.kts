plugins {
    java
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "Jubler"
    }
}

tasks.processResources {
    // Filter version.prop with project properties
    // Use filter to manually replace property references
    filesMatching("**/version.prop") {
        filter { line ->
            line.replace("\${project.version}", project.version.toString())
                .replace("\${long.version}", rootProject.extra["longVersion"].toString())
                .replace("\${release.version}", rootProject.extra["releaseVersion"].toString())
        }
    }
}

// Rename artifact to "jubler" instead of "launcher"
base {
    archivesName.set("jubler")
}

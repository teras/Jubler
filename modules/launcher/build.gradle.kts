plugins {
    id("jubler.java-conventions")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "Jubler"
    }
}

tasks.processResources {
    // Substitute the real project version (derived from the git tag) into version.prop.
    // Captured as a provider so the version is resolved lazily at execution time, without the
    // filter action reaching back to `Task.project` (which is disallowed under the configuration cache).
    val appVersion = providers.provider { project.version.toString() }
    inputs.property("appVersion", appVersion)
    filesMatching("**/version.prop") {
        filter { line ->
            line.replace("\${project.version}", appVersion.get())
        }
    }
}

// Rename artifact to "jubler" instead of "launcher"
base {
    archivesName.set("jubler")
}

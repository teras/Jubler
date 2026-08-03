plugins {
    id("jubler.java-conventions")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "Jubler"
        // Let appenh reflect the X11 window id out of the Swing peer, so the xdg-desktop-portal
        // file dialog can be parented/centered on the app window (see PortalFileChooser#parentToken).
        // Honored by `java -jar`; a no-op off Linux/X11.
        attributes["Add-Opens"] = "java.desktop/java.awt java.desktop/sun.awt java.desktop/sun.awt.X11"
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

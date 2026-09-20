plugins {
    id("jubler.java-conventions")
}

dependencies {
    implementation(libs.minimal.json)
    implementation(project(":launcher"))
    implementation(libs.appenh) {
        exclude(group = "com.panayotis", module = "loadlib")
    }
    implementation(libs.arjs)

    // xdg-desktop-portal FileChooser backend (via appenh on Linux). Bundled on every platform;
    // used only when the portal is reachable, otherwise the Swing chooser is used.
    implementation(libs.dbus.core)
    runtimeOnly(libs.dbus.transport.unixsocket)

    // dbus-java logs through SLF4J. Without a provider on the classpath SLF4J prints its "No SLF4J
    // providers were found" banner at every start and silently drops the messages, so ship the simple
    // provider; it writes to stderr and is muted below INFO by default.
    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

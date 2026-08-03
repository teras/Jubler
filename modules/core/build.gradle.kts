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

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

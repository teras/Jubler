import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("jubler.java-conventions")
}

// A Jubler plugin module builds on top of the launcher + core APIs and is unit-tested with JUnit 5.
val libs = the<LibrariesForLibs>()

dependencies {
    "implementation"(project(":launcher"))
    "implementation"(project(":core"))

    "testImplementation"(libs.junit.jupiter)
    "testRuntimeOnly"(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

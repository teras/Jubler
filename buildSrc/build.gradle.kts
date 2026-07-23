plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Expose the version catalog (`libs`) accessors to the precompiled convention plugins.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

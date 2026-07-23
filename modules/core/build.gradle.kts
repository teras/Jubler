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

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

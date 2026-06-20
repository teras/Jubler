plugins {
    java
}

dependencies {
    implementation(project(":launcher"))
    implementation(project(":core"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.3")
}

tasks.test {
    useJUnitPlatform()
}

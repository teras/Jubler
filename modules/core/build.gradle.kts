plugins {
    java
}

dependencies {
    implementation("com.eclipsesource.minimal-json:minimal-json:${rootProject.extra["jsonVersion"]}")
    implementation(project(":launcher"))
    implementation("com.panayotis:appenh:0.8.0") {
        exclude(group = "com.panayotis", module = "loadlib")
    }
    implementation("com.panayotis:arjs:0.3.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
}

tasks.test {
    useJUnitPlatform()
}

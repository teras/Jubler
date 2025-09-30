plugins {
    java
}

dependencies {
    implementation(project(":launcher"))
    implementation(project(":core"))
    implementation("com.eclipsesource.minimal-json:minimal-json:${rootProject.extra["jsonVersion"]}")
    implementation("com.panayotis:appenh:0.8.0")
}

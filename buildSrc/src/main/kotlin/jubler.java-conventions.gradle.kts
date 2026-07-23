plugins {
    java
}

group = "com.panayotis.jubler"
version = rootProject.version

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial"))
}

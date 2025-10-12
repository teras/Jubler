plugins {
    id("java")
    id("application")
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.panayotis.jubler.i18n.tools.I18nTools")
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.register<JavaExec>("extract") {
    group = "i18n"
    description = "Extract translatable strings from source code"
    dependsOn("classes")
    mainClass.set("com.panayotis.jubler.i18n.tools.I18nTools")
    classpath = sourceSets["main"].runtimeClasspath
    args("extract")
}

tasks.register<JavaExec>("merge") {
    group = "i18n"
    description = "Merge extracted strings with existing translations"
    dependsOn("classes")
    mainClass.set("com.panayotis.jubler.i18n.tools.I18nTools")
    classpath = sourceSets["main"].runtimeClasspath
    args("merge")
}

tasks.register("update") {
    group = "i18n"
    description = "Extract and merge translations (extract + merge)"
    dependsOn("extract", "merge")
}

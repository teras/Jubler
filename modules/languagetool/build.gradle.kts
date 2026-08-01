plugins {
    id("jubler.plugin-conventions")
}

dependencies {
    implementation(libs.languagetool.core)
    implementation(libs.languagetool.en)
    implementation(libs.slf4j.simple)
}

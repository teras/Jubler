plugins {
    id("jubler.plugin-conventions")
}

dependencies {
    implementation(project(":subdownload"))
    implementation(libs.minimal.json)
}

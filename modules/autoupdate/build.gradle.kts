plugins {
    id("jubler.plugin-conventions")
}

dependencies {
    implementation(libs.minimal.json)
    implementation(libs.appenh) {
        exclude(group = "com.panayotis", module = "loadlib")
    }
}

plugins {
    id("jubler.java-conventions")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.appenh) {
        exclude(group = "com.panayotis", module = "loadlib")
    }
}

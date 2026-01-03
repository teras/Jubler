plugins {
    java
}

dependencies {
    implementation(project(":core"))
    implementation("com.panayotis:appenh:0.8.0") {
        exclude(group = "com.panayotis", module = "loadlib")
    }
}

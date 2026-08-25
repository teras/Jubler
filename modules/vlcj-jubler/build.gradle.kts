plugins {
    id("jubler.plugin-conventions")
}

dependencies {
    implementation(libs.vlcj)
    // Pin JNA: vlcj 4.7.3 pulls JNA 5.11.0, which aborts the JVM with an assert
    // in dispatch.c when dlopen() fails with a long error message (macOS 12+),
    // e.g. when libvlc cannot be loaded. Fixed in JNA 5.12 (java-native-access/jna#1452).
    implementation(libs.jna)
    implementation(libs.jna.platform)
}

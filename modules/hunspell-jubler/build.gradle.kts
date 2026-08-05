plugins {
    id("jubler.plugin-conventions")
}

dependencies {
    // Bundles libhunspell natives for macOS (x86-64/aarch64), Linux (x86-64) and Windows (x86-64)
    // and pulls JNA transitively. On Linux aarch64 (no bundled native) it falls back to a system
    // libhunspell — which the Flatpak runtime provides.
    implementation(libs.hunspell)
}

plugins {
    id("jubler.plugin-conventions")
}

// LanguageTool drags in a large transitive tree for features Jubler never uses: remote/neural rules
// over gRPC, n-gram (confusion-pair) rules over Lucene, Prometheus/Micrometer/OpenTelemetry telemetry,
// and assorted JSON libraries. Jubler only runs the local grammar checker (JLanguageTool / RuleMatch),
// so these are pure bundle weight (~24 MB of jars). Excluding them leaves the check identical — same
// matches and the same 5814 active en-US rules — verified against the built classpath.
// Kept on purpose: grpc-api (io.grpc.Channel is referenced on the check path) and opentelemetry-api /
// opentelemetry-context (TelemetryProvider touches GlobalOpenTelemetry during check).
fun ExternalModuleDependency.trimLanguageTool() {
    // gRPC remote-rule transport: drop the giant shaded-netty transport and the protobuf marshallers.
    // Keep the small grpc-api/core/stub/util/context (~0.8 MB) — io.grpc.Channel is on the check path
    // and grpc-api is only reachable transitively through grpc-core.
    exclude(group = "io.grpc", module = "grpc-netty-shaded")
    exclude(group = "io.grpc", module = "grpc-protobuf")
    exclude(group = "io.grpc", module = "grpc-protobuf-lite")
    exclude(group = "com.google.protobuf", module = "protobuf-java")
    exclude(group = "com.google.api.grpc", module = "proto-google-common-protos")
    // n-gram / confusion-pair rules (require an explicitly configured n-gram data dir)
    exclude(group = "org.apache.lucene")
    // telemetry (keep opentelemetry-api / -context)
    exclude(group = "io.micrometer")
    exclude(group = "io.prometheus")
    exclude(group = "io.github.resilience4j")
    exclude(group = "io.opentelemetry", module = "opentelemetry-semconv")
    exclude(group = "org.hdrhistogram", module = "HdrHistogram")
    exclude(group = "org.latencyutils", module = "LatencyUtils")
    // JSON / serialization libs pulled for remote rules and config
    exclude(group = "com.fasterxml.jackson.core")
    exclude(group = "com.google.code.gson", module = "gson")
    exclude(group = "org.json", module = "json")
    exclude(group = "net.arnx", module = "jsonic")
    exclude(group = "com.eclipsesource.minimal-json", module = "minimal-json")
    exclude(group = "com.sun.xml.fastinfoset", module = "FastInfoset")
    exclude(group = "org.jvnet.staxex", module = "stax-ex")
    // misc unused by the local checker (language is chosen explicitly, so no auto-detection)
    exclude(group = "com.optimaize.languagedetector", module = "language-detector")
    exclude(group = "io.vavr")
    exclude(group = "com.hankcs", module = "aho-corasick-double-array-trie")
    exclude(group = "io.github.java-diff-utils", module = "java-diff-utils")
    exclude(group = "commons-validator", module = "commons-validator")
    exclude(group = "commons-digester", module = "commons-digester")
    exclude(group = "org.apache.commons", module = "commons-text")
    exclude(group = "org.apache.commons", module = "commons-pool2")
    exclude(group = "org.checkerframework", module = "checker-qual")
    exclude(group = "com.gitlab.dumonts", module = "hunspell")
    exclude(group = "edu.washington.cs.knowitall", module = "openregex")
}

dependencies {
    implementation(libs.languagetool.core) { trimLanguageTool() }
    implementation(libs.languagetool.en) { trimLanguageTool() }
    implementation(libs.slf4j.simple)
}

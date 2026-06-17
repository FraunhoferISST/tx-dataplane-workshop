plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
    alias(libs.plugins.docker)
}

dependencies {
    implementation(libs.edc.spi.web)

    implementation(libs.edc.core.token)

    implementation(libs.edc.lib.token)
    implementation(libs.edc.lib.keys)
    implementation(libs.edc.lib.oauth2.authn)

    implementation(libs.jakarta.rsApi)

    runtimeOnly(libs.edc.core.boot)
    runtimeOnly(libs.edc.core.runtime)
    runtimeOnly(libs.edc.core.connector)
    runtimeOnly(libs.edc.core.http)
    runtimeOnly(libs.edc.api.observability)
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveFileName.set("dataplane.jar")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

configurations {
    annotationProcessor {
        exclude(group = "org.eclipse.edc", module = "autodoc-processor")
    }
}

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
    alias(libs.plugins.docker)
}

dependencies {
    implementation(libs.edc.issuance.spi) // for seeding the attestations

    runtimeOnly(libs.edc.bom.issuerservice) {
        exclude(group = "org.eclipse.edc", module = "identityhub-api-authentication")
        exclude(group = "org.eclipse.edc", module = "identityhub-api-authorization")
        exclude(group = "org.eclipse.edc", module = "issuer-admin-api-authentication")
    }
    runtimeOnly(libs.edc.ih.api.did)
    runtimeOnly(libs.edc.ih.api.participants)
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.bom.issuerservice.sql)
    runtimeOnly(libs.edc.core.participantcontext.config)
    runtimeOnly(libs.edc.store.participantcontext.config.sql)
    runtimeOnly(libs.edc.monitor.console)
    runtimeOnly(libs.edc.monitor.otel)
    runtimeOnly(libs.edc.vault.transit)

    runtimeOnly(libs.opentelemetry.exporter.otlp)
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveFileName.set("issuerservice.jar")
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

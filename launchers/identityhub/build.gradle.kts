plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
    alias(libs.plugins.docker)
}

dependencies {
    runtimeOnly(libs.edc.bom.identityhub) {
        exclude(group = "org.eclipse.edc", module = "identityhub-api-authentication")
        exclude(group = "org.eclipse.edc", module = "identityhub-api-authorization")
    }
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.bom.identityhub.sql)
    runtimeOnly(libs.edc.core.participantcontext.config)
    runtimeOnly(libs.edc.store.participantcontext.config.sql)
    runtimeOnly(libs.edc.monitor.console)
    runtimeOnly(libs.edc.monitor.otel)
    runtimeOnly(libs.edc.vault.transit)

    runtimeOnly(libs.opentelemetry.exporter.otlp)
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveFileName.set("identityhub.jar")
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

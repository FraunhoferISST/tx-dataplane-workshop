plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
    alias(libs.plugins.docker)
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.tx.spi.bdrs)

    runtimeOnly(libs.tx.edc.cp) {
        // replace with the new dataplane signaling protocol
        exclude(group = "org.eclipse.edc", module = "transfer-data-plane-signaling")

        // exclude BDRS client as it's mocked
        exclude(group = "org.eclipse.tractusx.edc", module = "bdrs-client")

        // needs to be excluded as identity hub requires "org.eclipse.dspace.dcp.vc.type" in scope; default scopes are instead added via config
        exclude(group = "org.eclipse.tractusx.edc", module = "cx-dcp")

        // exclude as not needed with new dps impl
        exclude(group = "org.eclipse.tractusx.edc", module = "edr-core")
        exclude(group = "org.eclipse.tractusx.edc", module = "edr-api-v2")
        exclude(group = "org.eclipse.tractusx.edc", module = "edr-callback")
        exclude(group = "org.eclipse.tractusx.edc", module = "edr-index-lock-sql")
        exclude(group = "org.eclipse.tractusx.edc", module = "tokenrefresh-handler")
    }

    // dataplane signaling implementation
    runtimeOnly(libs.edc.dps)
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveFileName.set("controlplane.jar")
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

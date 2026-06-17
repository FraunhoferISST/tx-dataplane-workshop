pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

rootProject.name = "dataplane-workshop"

include(":launchers")
include(":launchers:controlplane")
include(":launchers:dataplane")
include(":launchers:identityhub")
include(":launchers:issuerservice")

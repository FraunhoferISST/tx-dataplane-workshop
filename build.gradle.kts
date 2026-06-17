import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.4.1"
    id("com.bmuschko.docker-remote-api") version "10.0.0"
    alias(libs.plugins.edc.build)
}

buildscript {
    dependencies {
        classpath("org.eclipse.edc.autodoc:org.eclipse.edc.autodoc.gradle.plugin:0.16.0")
    }
}

val edcBuildId = libs.plugins.edc.build.get().pluginId

val downloadOtelAgent by tasks.register("downloadOtelAgent", Copy::class) {
    val openTelemetry = configurations.create("open-telemetry")

    dependencies {
        openTelemetry(libs.opentelemetry.javaagent)
    }

    from(openTelemetry)
    into("build/otel")
    rename { "opentelemetry-javaagent.jar" }
}

allprojects {
    apply(plugin = edcBuildId)
    apply(plugin = "org.eclipse.edc.autodoc")

    configure<org.eclipse.edc.plugins.autodoc.AutodocExtension> {
        outputDirectory.set(project.layout.buildDirectory.asFile)
    }
}

subprojects {
    afterEvaluate {
        if (project.plugins.hasPlugin("com.github.johnrengelman.shadow") &&
            file("${project.projectDir}/src/main/docker/Dockerfile").exists()
        ) {
            apply(plugin = "com.bmuschko.docker-remote-api")

            val copyOtelAgent = tasks.register<Copy>("copyOtelAgent") {
                dependsOn(rootProject.tasks.named("downloadOtelAgent"))
                from(rootProject.layout.buildDirectory.dir("otel"))
                into(project.layout.buildDirectory.dir("otel"))
            }
            var shadowJarTask = tasks.named("shadowJar").get()
            shadowJarTask.dependsOn(copyOtelAgent)

            // configure the "dockerize" task
            val dockerTask: DockerBuildImage = tasks.create("dockerize", DockerBuildImage::class) {
                val dockerContextDir = project.projectDir
                dockerFile.set(file("$dockerContextDir/src/main/docker/Dockerfile"))

                if (System.getProperty("platform") != null)
                    platform.set(System.getProperty("platform"))
                buildArgs.put("JAR", "build/libs/${project.name}.jar")
                buildArgs.put("OTEL_AGENT", "build/otel/opentelemetry-javaagent.jar")
                inputDir.set(file(dockerContextDir))
                images.add("${project.name}:latest")
            }
            dockerTask.dependsOn(tasks.named("shadowJar"))
        }
    }
}

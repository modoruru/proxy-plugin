plugins {
    id("java")
    id("com.gradleup.shadow").version("9.6.0")
}

group = "modoru.proxy"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }

    // This guy publishes Velocity backend implementations on huge internal API changes
    maven("https://repo.william278.net/velocity/") {
        name = "custom-velocity"
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:${property("velocity_version")}")
    compileOnly("com.velocitypowered:velocity-proxy:${property("velocity_backend_version")}")
    compileOnly("io.netty:netty-codec-http:4.2.2.Final")
    compileOnly("net.kyori:adventure-nbt:4.23.0")
    annotationProcessor("com.velocitypowered:velocity-api:${property("velocity_version")}")

    implementation("net.elytrium:serializer:${property("serializer_version")}")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("velocity-plugin.json") {
            expand(mapOf("version" to project.version))
        }
    }
}

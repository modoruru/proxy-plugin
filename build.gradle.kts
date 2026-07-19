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
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:${property("velocity_version")}")
    annotationProcessor("com.velocitypowered:velocity-api:${property("velocity_version")}")
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

plugins {
    id("java")
    id("com.gradleup.shadow").version("9.0.0-beta4")
}

group = "modoru.proxy"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")
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

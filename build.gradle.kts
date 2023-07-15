plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"

    id("io.freefair.lombok") version "8.1.0"
}

group = "dev.emortal.velocity"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://repo.emortal.dev/snapshots")
    maven("https://repo.emortal.dev/releases")

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    // Metrics
    implementation("io.pyroscope:agent:0.11.5")

    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.1")
    implementation("net.kyori:adventure-text-minimessage:4.14.0")

    implementation("dev.emortal.api:common-proto-sdk:8572819")
    implementation("dev.emortal.api:agones-sdk:1.0.7")
    implementation("dev.emortal.api:live-config-parser:e26df7a")

    implementation("io.kubernetes:client-java:18.0.0")

    compileOnly("org.jetbrains:annotations:24.0.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(20))
    }
}

tasks {
    compileJava {
        options.compilerArgs.addAll(listOf(
                "--release", "20",
                "--enable-preview"
        ))
    }
    shadowJar {
        mergeServiceFiles()
    }
    test {
        useJUnitPlatform()
    }
}

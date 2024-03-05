plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    jacoco
}

group = "dev.emortal.velocity"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://repo.emortal.dev/snapshots")
    maven("https://repo.emortal.dev/releases")

    maven("https://repo.papermc.io/repository/maven-snapshots/")
    maven("https://packages.confluent.io/maven/")
    maven("https://repo.viaversion.com")
}

dependencies {
    // Metrics
    implementation("io.pyroscope:agent:0.12.0")

    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-proxy:3.2.0-SNAPSHOT")
    compileOnly("com.viaversion:viaversion-api:4.9.2")
    annotationProcessor("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    implementation("net.kyori:adventure-text-minimessage:4.14.0")

    implementation("dev.emortal.api:common-proto-sdk:cfb6fa4")
    implementation("dev.emortal.api:agones-sdk:1.0.7")
    implementation("dev.emortal.api:live-config-parser:8f566b9")
    implementation("dev.emortal.api:module-system:1.0.0")
    implementation("dev.emortal.api:command-system:1.0.0") {
        exclude(group = "com.mojang", module = "brigadier") // use Velocity Brigadier provided by Velocity
    }

    implementation("io.kubernetes:client-java:18.0.1")

    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    compileOnly("org.jetbrains:annotations:24.0.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
    testImplementation("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    testRuntimeOnly("org.slf4j:slf4j-nop:2.0.9")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    compileJava {
        options.compilerArgs.addAll(listOf(
                "--release", "21"
        ))
    }
    shadowJar {
        mergeServiceFiles()
    }
    test {
        useJUnitPlatform()
    }
    jacocoTestReport {
        dependsOn(test)
    }
}

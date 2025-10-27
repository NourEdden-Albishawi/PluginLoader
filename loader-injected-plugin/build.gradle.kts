plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "${parent?.group}"
version = "${parent?.version}"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot API repo
}

dependencies {
    // We no longer depend on loader-plugin, it's a standalone plugin now.
    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT") // Using a modern API version
}

// Configure shadowJar to produce a standard plugin JAR
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("all")
}

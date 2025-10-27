import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "${parent?.group}"
version = "${parent?.version}"

application {
    mainClass = "me.akraml.loader.LoaderBackend"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":loader-common"))
    implementation("org.yaml:snakeyaml:1.33")
}

// This is needed to include the dependency in the shadowJar
tasks.withType<ShadowJar> {
    relocate("org.yaml.snakeyaml", "me.akraml.loader.libs.snakeyaml")
}

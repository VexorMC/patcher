plugins {
    kotlin("jvm") version "2.0.20"
    id("dev.lunasa.patcher")
}

group = "dev.lunasa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.legacyfabric.net/")
}

dependencies {
    mappings("net.legacyfabric:yarn:1.8.9+build.551:mergedv2")
}

patcher {
    minecraftVersion = "1.8.9"
}

kotlin { jvmToolchain(17) }
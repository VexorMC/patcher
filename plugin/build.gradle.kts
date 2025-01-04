plugins {
    kotlin("jvm") version "2.0.20"
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "dev.lunasa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())

    implementation("net.fabricmc:tiny-remapper:0.10.4")
    implementation("commons-codec:commons-codec:1.17.1")
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.vineflower:vineflower:1.10.1")
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
    implementation("com.nothome:javaxdelta:2.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

gradlePlugin {
    plugins {
        create("dev.lunasa.patcher") {
            id = "dev.lunasa.patcher"
            implementationClass = "dev.lunasa.patcher.Patcher"
        }
    }
}
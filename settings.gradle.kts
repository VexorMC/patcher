pluginManagement.repositories {
    maven("https://maven.fabricmc.net/")
    gradlePluginPortal()
    mavenCentral()
}

rootProject.name = "patcher"
includeBuild("plugin")
include("test-project")

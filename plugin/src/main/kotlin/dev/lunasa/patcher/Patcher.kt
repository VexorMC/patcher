package dev.lunasa.patcher

import dev.lunasa.patcher.constant.Constants
import dev.lunasa.patcher.extension.PatcherExtension
import dev.lunasa.patcher.model.MinecraftManifest
import dev.lunasa.patcher.model.MinecraftManifest.gson
import dev.lunasa.patcher.model.VersionData
import dev.lunasa.patcher.task.ApplyPatchesTask
import dev.lunasa.patcher.task.DownloadAndRemapJarTask
import dev.lunasa.patcher.task.DownloadAssetsTask
import dev.lunasa.patcher.task.GenSourcesTask
import dev.lunasa.patcher.task.GenerateDeltaTask
import dev.lunasa.patcher.task.GeneratePatchesTask
import dev.lunasa.patcher.task.RemapJarTask
import dev.lunasa.patcher.task.RunClientTask
import dev.lunasa.patcher.util.NativesTask
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.get
import java.io.File
import java.net.URI
import java.net.URL

class Patcher : Plugin<Project> {
    override fun apply(project: Project) {
        Constants.initialize(project)

        val extension = project.extensions.create("patcher", PatcherExtension::class.java)

        project.configurations.create("mappings")

        val minecraftSourceDir = File(project.projectDir, "src/minecraft/java")
        val minecraftResourceDir = File(project.projectDir, "src/minecraft/resources")
        val patchesSourceDir = File(project.projectDir, "src/main/patches")

        val sourceSets = project.extensions.getByType(JavaPluginExtension::class.java).sourceSets
        val mainSourceSet = sourceSets.getByName("main")

        mainSourceSet.java.srcDirs(minecraftSourceDir, patchesSourceDir)
        mainSourceSet.resources.srcDirs(minecraftResourceDir)

        project.setupTasks()

        val jarTask = project.tasks.named("jar", Jar::class.java).get()
        val remapJarTask = project.tasks.register("remapJar", RemapJarTask::class.java) {
            dependsOn(jarTask)

            group = "Patcher"

            sourceNamespace.set("named")
            targetNamespace.set("official")

            inputJar.set(jarTask.archiveFile.get().asFile)
            outputJar.set(jarTask.archiveFile.get().asFile.parentFile.resolve(
                jarTask.archiveFile.get().asFile.nameWithoutExtension + "-remapped.jar"
            ))
        }
        val generateDeltaTask = project.tasks.register("generateDeltas", GenerateDeltaTask::class.java) {
            group = "Patcher"

            dependsOn(remapJarTask)

            modifiedJar.set(remapJarTask.get().outputs.files.singleFile)
            originalJar.set(Constants.Cache.resolve(extension.minecraftVersion.get()).resolve("client.jar"))

            outputJar.set(jarTask.archiveFile.get().asFile.parentFile.resolve(
                jarTask.archiveFile.get().asFile.nameWithoutExtension + ".jar"
            ))
        }

        project.tasks.named("jar", Jar::class.java) {
            finalizedBy(generateDeltaTask)
        }.get()

        project.afterEvaluate { afterEvaluate(extension) }
    }

    private fun Project.afterEvaluate(extension: PatcherExtension) {
        val mcVersion = extension.minecraftVersion.get()

        val mcManifest = MinecraftManifest.fromId(mcVersion) ?: throw RuntimeException("Unknown version specified ($mcVersion)")

        val manifest = gson.fromJson(
            URI.create(mcManifest.url).toURL().openStream().reader().readText(),
            VersionData::class.java
        ) ?: throw RuntimeException("Failed to fetch version manifest")

        NativesTask.downloadAndExtractNatives(project, manifest)

        repositories.add(
            repositories.maven {
                url = project.uri("https://libraries.minecraft.net/")
            }
        )

        manifest.libraries.forEach {
            if (!it.name.contains("platform")) {
                project.logger.info("[Patcher] Registering library ${it.name}")
                project.dependencies.add("implementation", it.name)
            }
        }
    }

    private fun Project.setupTasks() {
        val minecraftSourceDir = File(project.projectDir, "src/minecraft/java")
        val minecraftResourceDir = File(project.projectDir, "src/minecraft/resources")
        val patchesSourceDir = File(project.projectDir, "src/main/patches")

        val downloadAndRemapJarTask = tasks.register("downloadAndRemapJar", DownloadAndRemapJarTask::class.java) {
            group = "Patcher"

            outputResourceDirectory.set(minecraftResourceDir)
        }

        val genSourcesTask = tasks.register("genSources", GenSourcesTask::class.java) {
            group = "Patcher"

            inputJar.set(downloadAndRemapJarTask.get().remappedJar)
            outputFolder.set(minecraftSourceDir)
            cacheOutputFolder.set(Constants.Sources)

            dependsOn(downloadAndRemapJarTask)
        }

        val applyPatchesTask = tasks.register("applyPatches", ApplyPatchesTask::class.java) {
            group = "Patcher"

            if (!patchesSourceDir.exists()) patchesSourceDir.mkdirs()
            if (!minecraftSourceDir.exists()) minecraftSourceDir.mkdirs()

            patchesDir.set(patchesSourceDir)
            targetDir.set(minecraftSourceDir)

            dependsOn(genSourcesTask)
        }

        tasks.register("generatePatches", GeneratePatchesTask::class.java) {
            group = "Patcher"

            originalSrcDir.set(Constants.Sources)
            modifiedSrc.set(minecraftSourceDir)
            patchesDir.set(patchesSourceDir)
        }

        tasks.register("setupEnv") {
            group = "Patcher"

            dependsOn(applyPatchesTask)
        }

        val downloadAssetsTask = tasks.register("downloadAssets", DownloadAssetsTask::class.java) { group = "Patcher" }

        tasks.register("runClient", RunClientTask::class.java) {
            group = "Patcher"

            dependsOn(project.tasks.named("processResources"))
            dependsOn(project.tasks.withType(AbstractCompile::class.java).matching { that ->
                !that.name.lowercase().contains("test")
            })

            mainClass.set("net.minecraft.client.main.Main")

            dependsOn(downloadAssetsTask)
        }
    }
}
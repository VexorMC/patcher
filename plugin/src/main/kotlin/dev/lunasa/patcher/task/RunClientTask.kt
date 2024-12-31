package dev.lunasa.patcher.task

import dev.lunasa.patcher.constant.Constants
import dev.lunasa.patcher.extension.PatcherExtension
import dev.lunasa.patcher.model.MinecraftManifest
import dev.lunasa.patcher.model.MinecraftManifest.gson
import dev.lunasa.patcher.model.VersionData
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import java.net.URI

abstract class RunClientTask : JavaExec() {
    private val extension = project.extensions.getByType(PatcherExtension::class.java)

    @TaskAction
    override fun exec() {
        val mcManifest = MinecraftManifest.fromId(extension.minecraftVersion.get()) ?: throw RuntimeException("Unknown version specified (${extension.minecraftVersion.get()})")

        val manifest = gson.fromJson(
            URI.create(mcManifest.url).toURL().openStream().reader().readText(),
            VersionData::class.java
        ) ?: throw RuntimeException("Failed to fetch version manifest")


        jvmArgs("-Djava.library.path=${Constants.Cache.resolve("${manifest.id}/natives")}")

        args("--version=patcher")
        args("--accessToken=0")

        val runDirectory = project.projectDir.resolve("run")
        val assetsDirectory = Constants.Cache.resolve("${manifest.id}/assets")

        if (!runDirectory.exists()) runDirectory.mkdirs()

        workingDir(runDirectory)

        args("--gameDir", runDirectory.absolutePath)
        args("--assetsDir", assetsDirectory.absolutePath)
        args("--userType", "mojang")
        args("--assetIndex", "1.8")
        args("--uuid", "N/A")

        val sourceSets = project.extensions.findByType(org.gradle.api.plugins.JavaPluginExtension::class.java)?.sourceSets
        val compiledJava = sourceSets?.flatMap { it.output.files }
        val resources = sourceSets?.flatMap { it.resources }

        resources?.forEach {
            logger.lifecycle(it.absolutePath)
        }

        classpath(
            project.configurations.getByName("runtimeClasspath"),
            compiledJava,
            resources?.map { it.parentFile },
            project.objects.fileCollection()
        )

        if(!runDirectory.exists()) runDirectory.mkdirs()

        super.exec()
    }
}
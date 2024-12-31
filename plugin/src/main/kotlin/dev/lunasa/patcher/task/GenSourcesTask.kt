package dev.lunasa.patcher.task

import dev.lunasa.patcher.decompile.VineFlowerDecompiler
import dev.lunasa.patcher.util.extractZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Not worth caching")
abstract class GenSourcesTask : DefaultTask() {
    @get:InputFile
    abstract val inputJar: Property<File>
    @get:OutputDirectory
    abstract val outputFolder: Property<File>
    @get:OutputDirectory
    abstract val cacheOutputFolder: Property<File>

    @TaskAction
    fun decompile() {
        logger.lifecycle("[Patcher] Decompiling")

        val tempFile = File.createTempFile("patcher-decompile", ".jar")
        VineFlowerDecompiler.decompile(
            project,
            inputJar.get(),
            tempFile,
        )

        extractZipFile(tempFile.path, cacheOutputFolder.get().path, listOf("META-INF"))
        extractZipFile(tempFile.path, outputFolder.get().path, listOf("META-INF"))

        logger.lifecycle("[Patcher] Decompiled and saved successfully")
    }
}
package dev.lunasa.patcher.task

import at.spardat.xma.xdelta.JarDelta
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

@DisableCachingByDefault(because = "Not worth caching")
abstract class GenerateDeltaTask : DefaultTask() {
    @get:InputFile
    abstract val modifiedJar: Property<File>
    @get:InputFile
    abstract val originalJar: Property<File>

    @get:OutputFile
    abstract val outputJar: Property<File>

    @TaskAction
    fun generateDeltas() {
        val jarDelta = JarDelta()

        val originalZip = JarFile(originalJar.get())
        val modifiedZip = JarFile(modifiedJar.get())

        if (outputJar.get().exists()) outputJar.get().delete()

        jarDelta.computeDelta(originalZip, modifiedZip, ZipOutputStream(outputJar.get().outputStream()))
    }
}
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
import com.google.common.io.ByteStreams
import com.google.common.io.ByteArrayDataOutput
import com.nothome.delta.Delta
import java.util.zip.ZipEntry

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
        val delta = Delta()

        val originalZip = JarFile(originalJar.get())
        val modifiedZip = JarFile(modifiedJar.get())

        if (outputJar.get().exists()) outputJar.get().delete()

        val outputStream = ZipOutputStream(outputJar.get().outputStream())

        val patches = mutableListOf<ByteArray>()

        originalZip.entries().asSequence().forEach { originalEntry ->
            val modifiedEntry = modifiedZip.getEntry(originalEntry.name)

            if (modifiedEntry != null) {
                val cleanBytes = ByteStreams.toByteArray(originalZip.getInputStream(originalEntry))
                val modifiedBytes = ByteStreams.toByteArray(modifiedZip.getInputStream(modifiedEntry))

                val deltaBytes = delta.compute(cleanBytes, modifiedBytes)

                val className = originalEntry.name.replace("/", ".").removeSuffix(".class")

                val output: ByteArrayDataOutput = ByteStreams.newDataOutput()

                output.writeUTF(className)           // Class name
                output.writeUTF(originalEntry.name)  // Clean class name
                output.writeUTF(modifiedEntry.name)  // Modified class name

                output.writeInt(deltaBytes.size)
                output.write(deltaBytes)

                patches.add(output.toByteArray())
            }
        }

        patches.forEach { patch ->
            val patchEntry = ZipEntry("patch/patch_${patches.indexOf(patch)}.bin")
            outputStream.putNextEntry(patchEntry)
            outputStream.write(patch)
            outputStream.closeEntry()
        }

        outputStream.close()
    }
}

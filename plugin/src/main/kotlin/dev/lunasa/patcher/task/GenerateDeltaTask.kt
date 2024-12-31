package dev.lunasa.patcher.task

import com.nothome.delta.Delta
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.com.google.common.io.ByteArrayDataOutput
import org.gradle.internal.impldep.com.google.common.io.ByteStreams
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipEntry
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
        val delta = Delta()
        var counter = 0
        val patchEntries = mutableListOf<ByteArray>()

        val originalZip = JarFile(originalJar.get())
        val modifiedZip = JarFile(modifiedJar.get())

        if (outputJar.get().exists()) outputJar.get().delete()

        val outputStream = ZipOutputStream(outputJar.get().outputStream())

        modifiedZip.entries().asSequence().forEach { modifiedEntry ->
            val originalEntry = originalZip.getEntry(modifiedEntry.name)

            if (originalEntry != null) {
                val cleanBytes = ByteStreams.toByteArray(originalZip.getInputStream(originalEntry))
                val modifiedBytes = ByteStreams.toByteArray(modifiedZip.getInputStream(modifiedEntry))

                if (!cleanBytes.contentEquals(modifiedBytes)) {
                    val deltaBytes = delta.compute(cleanBytes, modifiedBytes)

                    val className = modifiedEntry.name.replace("/", ".").removeSuffix(".class")

                    val output: ByteArrayDataOutput = ByteStreams.newDataOutput()

                    output.writeUTF(className)           // Class name
                    output.writeUTF(originalEntry.name)  // Clean class name
                    output.writeUTF(modifiedEntry.name)  // Modified class name

                    output.writeInt(deltaBytes.size)
                    output.write(deltaBytes)

                    patchEntries.add(output.toByteArray())
                    counter++
                }
            } else {
                val modifiedInputStream = modifiedZip.getInputStream(modifiedEntry)
                val zipEntry = ZipEntry(modifiedEntry.name)
                outputStream.putNextEntry(zipEntry)
                modifiedInputStream.copyTo(outputStream)
                outputStream.closeEntry()
            }
        }

        if (patchEntries.isNotEmpty()) {
            val patchZipEntry = ZipEntry("patches.zip")
            outputStream.putNextEntry(patchZipEntry)

            val patchZipStream = ZipOutputStream(outputStream)
            patchEntries.forEach { patchData ->
                val patchEntry = ZipEntry("patch_${counter++}.bin")
                patchZipStream.putNextEntry(patchEntry)
                patchZipStream.write(patchData)
                patchZipStream.closeEntry()
            }

            outputStream.closeEntry()
        }

        outputStream.close()
    }
}

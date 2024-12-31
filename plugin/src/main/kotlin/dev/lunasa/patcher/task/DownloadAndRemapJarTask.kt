package dev.lunasa.patcher.task

import dev.lunasa.patcher.constant.Constants
import dev.lunasa.patcher.extension.PatcherExtension
import dev.lunasa.patcher.model.MinecraftManifest
import dev.lunasa.patcher.model.MinecraftManifest.gson
import dev.lunasa.patcher.model.VersionData
import dev.lunasa.patcher.util.Downloader
import dev.lunasa.patcher.util.Mapper
import dev.lunasa.patcher.util.extractZipFile
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.zip.ZipInputStream

abstract class DownloadAndRemapJarTask : DefaultTask() {
    private val minecraftVersion: Property<String>
        get() = project.extensions.getByType(PatcherExtension::class.java).minecraftVersion

    @OutputFile
    val remappedJar = Constants.Remapped.resolve("client-${minecraftVersion.get()}-remapped.jar")

    @get:OutputDirectory
    abstract val outputResourceDirectory: Property<File>

    private lateinit var manifest: VersionData
    private lateinit var downloadedJar: File

    @TaskAction
    fun fetchManifests() {
        project.logger.lifecycle("[Patcher] Fetching version manifests")

        minecraftVersion.orNull ?: throw GradleException("Version must be set")

        val mcManifest = MinecraftManifest.fromId(minecraftVersion.get())
            ?: throw RuntimeException("Unknown version specified (${minecraftVersion.get()})")

        manifest = gson.fromJson(
            URL(mcManifest.url).openStream().reader().readText(),
            VersionData::class.java
        ) ?: throw RuntimeException("Failed to fetch version manifest")

        downloadedJar = Constants.Cache.resolve(minecraftVersion.get()).resolve("client.jar")

        project.logger.lifecycle("[Patcher] Downloading client")

        Downloader.download(
            manifest.downloads.client.url, downloadedJar,
            manifest.downloads.client.sha1
        ) {
            val totalBoxes = 30
            val neededBoxes = (it * totalBoxes).toInt()
            val characters = "=".repeat(neededBoxes)
            val whitespaces = " ".repeat(totalBoxes - neededBoxes)

            print(
                "[Patcher] Client: [$characters$whitespaces] (${it})\r"
            )
        }
        val mappings = Mapper.extractTinyMappingsFromJar(
            project.configurations.getByName("mappings").resolve().toList()[0]
        )

        project.logger.lifecycle("[Patcher] Remapping client")

        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappings,
                "official", "named"))
            .skipLocalVariableMapping(true)
            .renameInvalidLocals(true)
            .inferNameFromSameLvIndex(true)
            .ignoreConflicts(true)
            .ignoreFieldDesc(true)
            .resolveMissing(false)
            .build()

        try {
            OutputConsumerPath.Builder(remappedJar.toPath()).build().use { outputConsumer ->
                outputConsumer.addNonClassFiles(downloadedJar.toPath(),
                    NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readInputs(downloadedJar.toPath())
                remapper.apply(outputConsumer)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            remapper.finish()
        }

        val resourceDir = outputResourceDirectory.get()

        val buffer = ByteArray(1024)

        if (!resourceDir.exists()) {
            resourceDir.mkdir()
        }

        ZipInputStream(FileInputStream(downloadedJar)).use { zis ->
            var zipEntry = zis.nextEntry

            while (zipEntry != null) {
                val newFile = File(resourceDir, zipEntry.name)

                if (!zipEntry?.name!!.contains(".class") || zipEntry?.name!!.contains("META-INF")) {
                    if(zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        File(newFile.parent).mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                } else {
                    println("Excluding non-resource file ${zipEntry.name}")
                }

                zipEntry = zis.nextEntry
            }
            zis.closeEntry()
        }
    }
}
package dev.lunasa.patcher.task

import dev.lunasa.patcher.constant.Constants
import dev.lunasa.patcher.extension.PatcherExtension
import dev.lunasa.patcher.util.Mapper
import dev.lunasa.patcher.util.openZipFileSystem
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException

@DisableCachingByDefault(because = "Not worth caching")
abstract class RemapJarTask : DefaultTask() {
    @get:InputFile
    abstract val inputJar: Property<File>

    @get:OutputFile
    abstract val outputJar: Property<File>

    @get:Input
    abstract val sourceNamespace: Property<String>

    @get:Input
    abstract val targetNamespace: Property<String>

    private val minecraftVersion: Property<String>
        get() = project.extensions.getByType(PatcherExtension::class.java).minecraftVersion

    private val downloadedJar = Constants.Remapped.resolve("client-${minecraftVersion.get()}-remapped.jar")

    @TaskAction
    fun remap() {
        val mappings = Mapper.extractTinyMappingsFromJar(
            project.configurations.getByName("mappings").resolve().toList()[0]
        )

        if(!downloadedJar.exists()) {
            throw GradleException("Cannot find obfuscated Minecraft. Please run :downloadAndRemapJar")
        }

        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappings, sourceNamespace.get(), targetNamespace.get()))
            .renameInvalidLocals(true)
            .ignoreConflicts(true)
            .threads(Runtime.getRuntime().availableProcessors())
            .build()

        val classpath = listOf(*project.configurations.getByName("runtimeClasspath")
            .map { it.toPath() }.toTypedArray(),
            // We need the obfuscated client jar in the classpath
            downloadedJar.toPath())

        logger.lifecycle("[Patcher/RemapJar] Remapping ${inputJar.get().nameWithoutExtension} from ${sourceNamespace.get()} to ${targetNamespace.get()}")

        val tag = remapper.createInputTag()

        logger.lifecycle("[Patcher/RemapJar] Tag: $tag")

        remapper.readClassPath(*classpath.toTypedArray())

        logger.lifecycle("[Patcher/RemapJar] Reading....")
        remapper.readInputs(tag, inputJar.get().toPath())

        logger.lifecycle("[Patcher/RemapJar] Writing....")
        try {
            OutputConsumerPath.Builder(outputJar.get().toPath()).build().use { outputConsumer ->
                outputConsumer.addNonClassFiles(inputJar.get().toPath(),
                    NonClassCopyMode.FIX_META_INF, remapper)
                remapper.apply(outputConsumer, tag)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        remapper.finish()

        logger.lifecycle("[Patcher/RemapJar] Remapped successfully")
    }
}
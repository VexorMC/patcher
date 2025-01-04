package dev.lunasa.patcher.task

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Not worth caching")
abstract class GeneratePatchesTask : DefaultTask() {
    @get:InputDirectory
    abstract val originalSrcDir: Property<File>

    @get:InputDirectory
    abstract val modifiedSrc: Property<File>

    @get:OutputDirectory
    abstract val patchesDir: Property<File>

    @TaskAction
    fun generatePatches() {
        val originalDir = originalSrcDir.get()
        val modifiedDir = modifiedSrc.get()
        val patchesDir = patchesDir.get()

        if (!patchesDir.exists()) {
            patchesDir.mkdirs()
        }

        originalDir.walk().forEach { originalFile ->
            if (originalFile.isFile) {
                val relativePath = originalDir.toPath().relativize(File(originalFile.parentFile, "${originalFile.name}.patch").toPath())
                val modifiedRel = originalDir.toPath().relativize(originalFile.toPath())
                val modifiedFilePath = File(modifiedDir, modifiedRel.toString())

                if (modifiedFilePath.exists()) {
                    val originalLines = originalFile.readLines()
                    val modifiedLines = modifiedFilePath.readLines()

                    val patch: Patch<String> = DiffUtils.diff(originalLines, modifiedLines)

                    if (patch.deltas.isNotEmpty()) {
                        val patchFilePath = patchesDir.toPath().resolve(relativePath).toFile()
                        patchFilePath.parentFile.mkdirs()

                        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                            originalFile.name,
                            modifiedFilePath.name,
                            originalLines,
                            patch,
                            5
                        )

                        patchFilePath.bufferedWriter().use { writer ->
                            unifiedDiff.forEach { writer.write(it + "\n") }
                        }

                        logger.lifecycle("[Patcher] Generated patch for ${modifiedFilePath.nameWithoutExtension}.java")
                    }
                }
            }
        }
    }
}

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
abstract class ApplyPatchesTask : DefaultTask() {
    @get:InputDirectory
    abstract val patchesDir: Property<File>

    @get:OutputDirectory
    abstract val targetDir: Property<File>

    @TaskAction
    fun applyPatches() {
        val patchesDir = patchesDir.get()
        val targetDir = targetDir.get()

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        patchesDir.walk().forEach { patchFile ->
            if (patchFile.isFile) {
                val relativePath = patchesDir.toPath().relativize(File(patchFile.parentFile, patchFile.name.replace(".patch", "")).toPath())
                val targetFilePath = File(targetDir, relativePath.toString())

                targetFilePath.parentFile.mkdirs()

                val patchLines = patchFile.readLines()

                val patch: Patch<String> = UnifiedDiffUtils.parseUnifiedDiff(patchLines)

                val originalFile = File(targetDir, relativePath.toString())
                if (originalFile.exists()) {
                    val originalLines = originalFile.readLines()

                    val result = DiffUtils.patch(originalLines, patch)

                    originalFile.writeText(result.joinToString("\n"))

                    logger.lifecycle("[Patcher] Applied patch to ${originalFile.name}")
                }
            }
        }
    }
}

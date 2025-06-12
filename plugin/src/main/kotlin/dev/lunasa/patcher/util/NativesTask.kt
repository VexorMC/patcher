package dev.lunasa.patcher.util

import dev.lunasa.patcher.constant.Constants
import dev.lunasa.patcher.model.VersionData
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import java.io.File

object NativesTask {
    fun downloadAndExtractNatives(project: Project, manifest: VersionData) {
        val nativesDirectory = Constants.Cache.resolve("${manifest.id}/natives")

        if(!nativesDirectory.exists()) nativesDirectory.mkdirs()

        manifest.libraries.forEach {
            val classifiers = it.downloads.classifiers

            if (classifiers != null) {
                val native = when (OperatingSystem.current()) {
                    OperatingSystem.WINDOWS -> classifiers.nativesWindows
                    OperatingSystem.MAC_OS -> classifiers.nativesOsx
                    OperatingSystem.LINUX -> classifiers.nativesLinux
                    else -> null
                }
                if (native != null) {
                    val file = File(nativesDirectory, native.path)
                    if (!file.exists() || file.length() != native.size) {
                        file.parentFile.mkdirs()
                        file.createNewFile()
                        Downloader.download(native.url, file) {
                            val totalBoxes = 30
                            val neededBoxes = (it * totalBoxes).toInt()
                            val characters = "=".repeat(neededBoxes)
                            val whitespaces = " ".repeat(totalBoxes - neededBoxes)

                            print(
                                "Minecraft ${manifest.id} Natives: [$characters$whitespaces] (${"%.2f".format(it * 100)}%)\r"
                            )
                        }
                        println("")
                        var excludeList = emptyList<String>()
                        if (it.extract != null) {
                            excludeList = it.extract.exclude
                        }
                        extractZipFile(
                            file.absolutePath, nativesDirectory.absolutePath,
                            excludeList)
                    }
                }
            }
        }
    }
}
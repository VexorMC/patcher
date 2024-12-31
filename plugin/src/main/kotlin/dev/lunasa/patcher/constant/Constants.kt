package dev.lunasa.patcher.constant

import org.gradle.api.Project
import java.io.File

object Constants {
    lateinit var ROOT: File

    val Cache get() = ensureExists { ROOT.resolve(".gradle").resolve("patcher-cache") }
    val Remapped get() = ensureExists { Cache.resolve("remapped") }
    val Sources get() = ensureExists { Cache.resolve("sources") }

    fun initialize(project: Project) {
        ROOT = project.rootDir
    }

    fun ensureExists(create: () -> File): File {
        val file = create()

        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }

        return file
    }
}
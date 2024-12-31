package dev.lunasa.patcher.task

import com.google.gson.JsonObject
import dev.lunasa.patcher.constant.Constants
import dev.lunasa.patcher.extension.PatcherExtension
import dev.lunasa.patcher.model.MinecraftManifest
import dev.lunasa.patcher.model.MinecraftManifest.gson
import dev.lunasa.patcher.model.VersionData
import dev.lunasa.patcher.util.Downloader
import kotlinx.coroutines.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

private const val RESOURCES_URL = "https://resources.download.minecraft.net/"

abstract class DownloadAssetsTask : DefaultTask() {
    @OutputDirectory
    val assetsDirectory: File = Constants.Cache.resolve("${minecraftVersion.get()}/assets")

    private val minecraftVersion: Property<String>
        get() = project.extensions.getByType(PatcherExtension::class.java).minecraftVersion

    @TaskAction
    fun downloadAssets() = runBlocking {
        val version = minecraftVersion.orNull
            ?: throw GradleException("Version must be set")

        val mcManifest = MinecraftManifest.fromId(version)
            ?: throw RuntimeException("Unknown version specified ($version)")

        val manifest = gson.fromJson(
            URL(mcManifest.url).readText(),
            VersionData::class.java
        ) ?: throw RuntimeException("Failed to fetch version manifest")

        val assetIndexContent = URL(manifest.assetIndex.url).readText()
        val assetIndexFile = assetsDirectory.resolve("indexes/${manifest.assetIndex.id}.json")

        if (!assetIndexFile.exists()) {
            assetIndexFile.parentFile.mkdirs()
            assetIndexFile.writeText(assetIndexContent)
        }

        val assetObjects = gson.fromJson(assetIndexContent, JsonObject::class.java)["objects"].asJsonObject
        val downloadedCount = AtomicInteger(0)
        val totalAssets = assetObjects.size()

        coroutineScope {
            assetObjects.entrySet().map { (_, value) ->
                async(Dispatchers.IO) {
                    downloadAssetAsync(value.asJsonObject, downloadedCount, totalAssets)
                }
            }.awaitAll()
        }

        logger.lifecycle("[Patcher] All assets downloaded successfully.")
    }

    private suspend fun downloadAssetAsync(
        asset: JsonObject,
        downloadedCount: AtomicInteger,
        totalAssets: Int
    ) = withContext(Dispatchers.IO) {
        val hash = asset["hash"].asString
        val size = asset["size"].asLong
        val folder = assetsDirectory.resolve("objects/${hash.substring(0, 2)}")
        val file = File(folder, hash)

        if (!file.exists() || file.length() != size || !Downloader.hash(file).equals(hash, ignoreCase = true)) {
            val url = "$RESOURCES_URL${hash.substring(0, 2)}/$hash"
            val response = URL(url).readBytes()

            file.parentFile.mkdirs()
            file.writeBytes(response)

            val progress = downloadedCount.incrementAndGet().toFloat() / totalAssets
            updateProgress(progress)
        }
    }

    private fun updateProgress(progress: Float) {
        val totalBoxes = 30
        val filledBoxes = (progress * totalBoxes).toInt()
        val bar = "=".repeat(filledBoxes) + " ".repeat(totalBoxes - filledBoxes)
        logger.lifecycle("\r[Patcher] Assets: [$bar] (${"%.2f".format(progress * 100)}%)")
    }
}

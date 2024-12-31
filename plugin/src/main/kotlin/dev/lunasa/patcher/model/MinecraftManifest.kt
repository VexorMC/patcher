package dev.lunasa.patcher.model

import com.google.gson.Gson
import java.io.InputStreamReader
import java.net.URL

object MinecraftManifest {
    val gson = Gson()

    private val versionManifest = gson.fromJson(
        InputStreamReader(
            URL("https://piston-meta.mojang.com/mc/game/version_manifest.json")
                .openStream()
        ),
        VersionManifest::class.java
    )

    fun getAll(filter: (MVersion) -> Boolean = { true }): List<MVersion> {
        return versionManifest.versions
    }

    fun fromId(id: String): MVersion? {
        return getAll().find { it.id == id }
    }
}
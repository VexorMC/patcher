package dev.lunasa.patcher.example

import com.google.common.io.ByteArrayDataInput
import com.google.common.io.ByteStreams
import com.nothome.delta.GDiffPatcher
import net.minecraft.launchwrapper.LaunchClassLoader
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ClassPatcher {
    val patches = mutableMapOf<String, ClassPatchEntry>()
    val patchCache = mutableMapOf<String, ByteArray>()

    private val patcher = GDiffPatcher()

    private val logger = LogManager.getLogger("ClassPatcher")

    fun discoverPatches() {
        val jis = ZipInputStream(javaClass.getResourceAsStream("/patches.zip"))

        logger.info("Reading entries")
        do {
            try {
                logger.info("Reading entry")
                val entry = jis.nextEntry ?: break
                    val cp = readPatch(entry, jis)
                    cp.let {
                        patches.put(it.cleanName, it)
                    }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } while (true)

        logger.info("Discovered ${patches.size} binary patches")
    }

    fun readPatch(patchEntry: ZipEntry, jis: ZipInputStream): ClassPatchEntry {
        val input: ByteArrayDataInput
        try {
            input = ByteStreams.newDataInput(jis.readAllBytes())
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException("Unable to read binary patch file ${patchEntry.name}")
        }

        val name = input.readUTF()
        val cleanName = input.readUTF()
        val modifiedName = input.readUTF()

        val patchLength = input.readInt()
        val patchBytes = ByteArray(patchLength)

        input.readFully(patchBytes)

        return ClassPatchEntry(name, cleanName, modifiedName, patchBytes)
    }

    fun applyPatch(name: String, mappedName: String, inputData: ByteArray): ByteArray {
        if (patchCache.containsKey(name)) {
            return patchCache[name]!!
        }

        val patch = patches[mappedName]!!

        var transformed: ByteArray = inputData

        logger.info("Transforming $name")

        synchronized(patcher) {
            try {
                transformed = patcher.patch(inputData, patch.patchBytes)
                logger.info("Successfully transformed $name")
            } catch (e: IOException) {
                logger.error("Failed to patch $name", e)
            }
        }

        patchCache[name] = transformed

        return transformed
    }
}

data class ClassPatchEntry(
    val name: String,
    val cleanName: String,
    val modifiedName: String,
    val patchBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassPatchEntry

        if (name != other.name) return false
        if (cleanName != other.cleanName) return false
        if (modifiedName != other.modifiedName) return false
        if (!patchBytes.contentEquals(other.patchBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + cleanName.hashCode()
        result = 31 * result + modifiedName.hashCode()
        result = 31 * result + patchBytes.contentHashCode()
        return result
    }
}
package dev.lunasa.patcher.inject

import java.util.concurrent.CopyOnWriteArrayList

interface IClassTransformer {
    /**
     * Transforms the provided class bytes.
     */
    fun transform(name: String, transformedName: String, source: ByteArray): ByteArray
}

class TransformingClassLoader(
    parent: ClassLoader = getSystemClassLoader()
) : ClassLoader(parent) {

    private val transformers: MutableList<IClassTransformer> = CopyOnWriteArrayList()

    fun addTransformer(transformer: IClassTransformer) {
        transformers += transformer
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("sun.") || name.startsWith("jdk.") || name.startsWith("org.apache.logging.log4j.") || name.startsWith("com.google.common.") || findLoadedClass(name) != null) {
            return super.loadClass(name, resolve)
        }

        findLoadedClass(name)?.let { return it }

        return try {
            val clazz = findClass(name)
            if (resolve) {
                resolveClass(clazz)
            }
            clazz
        } catch (e: ClassNotFoundException) {
            super.loadClass(name, resolve)
        }
    }

    override fun findClass(name: String): Class<*> {
        val path = name.replace('.', '/').plus(".class")

        val classBytes = getResourceAsStream(path)?.use { it.readBytes() }
            ?: throw ClassNotFoundException("Class $name not found")

        val transformedBytes = applyTransformers(name, classBytes)

        return defineClass(name, transformedBytes, 0, transformedBytes.size)
    }

    private fun applyTransformers(name: String, bytes: ByteArray): ByteArray {
        var transformedBytes = bytes
        for (transformer in transformers) {
            transformedBytes = transformer.transform(name, name, transformedBytes)
        }
        return transformedBytes
    }
}

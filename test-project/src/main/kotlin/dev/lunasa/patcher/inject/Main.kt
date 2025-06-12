package dev.lunasa.patcher.inject

import dev.lunasa.patcher.example.ClassPatcher

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        ClassPatcher.discoverPatches()

        val classLoader = TransformingClassLoader()
        classLoader.addTransformer(GDiffTransformer())

        Thread.currentThread().setContextClassLoader(classLoader)

        classLoader.loadClass("net.minecraft.client.main.Main").getMethod("main", Array<String>::class.java)
            .invoke(null, arrayOf("client", "--version", "Patcher", "--accessToken", "0", "--assetIndex", "1.8", "--assetsDir", "assets"))
    }
}
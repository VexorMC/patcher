package dev.lunasa.patcher.inject

import dev.lunasa.patcher.example.ClassPatcher

class GDiffTransformer : IClassTransformer {
    override fun transform(name: String, transformedName: String, source: ByteArray): ByteArray {
        return ClassPatcher.applyPatch(name, transformedName, source)
    }
}
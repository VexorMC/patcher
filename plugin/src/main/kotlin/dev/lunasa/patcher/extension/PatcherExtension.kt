package dev.lunasa.patcher.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class PatcherExtension @Inject constructor(objects: ObjectFactory) {
    val minecraftVersion: Property<String> = objects.property(String::class.java)
}

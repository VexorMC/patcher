package dev.lunasa.patcher.decompile

import org.gradle.api.logging.Logger
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger

class VineFlowerLogger(private val logger: Logger) : IFernflowerLogger() {
    override fun writeMessage(p0: String, p1: Severity) {}
    override fun writeMessage(p0: String, p1: Severity, p2: Throwable) {}
}
package dev.lunasa.patcher.util

import org.gradle.api.logging.Logger

const val RESET = "\u001B[0m"
const val BLACK = "\u001B[30m"
const val CYAN_BG = "\u001B[46m"
const val RED_BG = "\u001B[41m"

fun lifecycle(logger: Logger, message: String, division: String? = null) {
    logger.lifecycle("$CYAN_BG$BLACK PATCHER${division?.let { " / ${it.uppercase()}" } ?: ""} $RESET $message$RESET")
}
fun error(logger: Logger, message: String, division: String? = null) {
    logger.error("$RED_BG$BLACK PATCHER${division?.let { " / ${it.uppercase()}" } ?: ""} $RESET $message$RESET")
}
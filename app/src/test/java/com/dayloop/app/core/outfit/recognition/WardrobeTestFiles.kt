package com.dayloop.app.core.outfit.recognition

import java.io.File

/** Locates files for JVM tests whether Gradle runs from the module or the repository root. */
internal object WardrobeTestFiles {
    fun shapeModel(): File = listOf("src/main/assets", "app/src/main/assets")
        .map { File(it, ShapeModelWeights.ASSET_PATH) }
        .firstOrNull { it.isFile }
        ?: error("Shape model asset not found from ${File(".").absolutePath}")

    fun resource(name: String): ByteArray =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "Missing test resource $name" }.use { it.readBytes() }
}

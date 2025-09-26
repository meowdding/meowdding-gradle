package me.owdding.gradle

import earth.terrarium.cloche.api.metadata.FabricMetadata
import org.gradle.api.provider.Provider

fun FabricMetadata.dependency(modId: String, version: Provider<String>? = null, untilNextMajor: Boolean = true) {
    dependency {
        it.modId.set(modId)
        it.required.set(true)
        if (version != null) it.version { versionRange ->
            versionRange.start.set(version)
            if (untilNextMajor) {
                versionRange.end.set(version.map { version -> "${version.substringAfter(".").toInt() + 1}.0.0" })
                versionRange.endExclusive.set(true)
            }
        }
    }
}

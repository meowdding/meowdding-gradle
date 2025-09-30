package me.owdding.gradle

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import earth.terrarium.cloche.api.metadata.FabricMetadata
import earth.terrarium.cloche.api.metadata.ModMetadata
import earth.terrarium.cloche.api.metadata.ModMetadata.Dependency
import earth.terrarium.cloche.api.target.FabricTarget
import earth.terrarium.cloche.api.target.MinecraftTarget
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.lambdas.SerializableLambdas.action
import org.gradle.api.provider.Provider

fun FabricMetadata.dependency(modId: String, version: Provider<String>? = null, untilNextMajor: Boolean = true) {
    dependency {
        it.modId.set(modId)
        it.required.set(true)
        if (version != null) it.version { versionRange ->
            versionRange.start.set(version)
            if (untilNextMajor) {
                versionRange.end.set(version.map { version -> "${version.substringBefore(".").toInt() + 1}.0.0" })
                versionRange.endExclusive.set(true)
            }
        }
    }
}

private val breaksMap: Multimap<FabricTarget, Dependency> = MultimapBuilder.hashKeys().arrayListValues().build()

internal fun MinecraftTarget.breaks(): Collection<Dependency> = (this as? FabricTarget)?.let { breaksMap.get(it) } ?: emptyList()

fun FabricTarget.breaks(modId: String, version: String) {
    breaks(modId, project.provider { version })
}

fun FabricTarget.breaks(modId: String, version: Provider<String>? = null) {
    breaks {
        it.modId.set(modId)
        it.required.set(true)
        if (version != null) it.version { versionRange ->
            versionRange.end.set(version)
            versionRange.endExclusive.set(false)
        }
    }
}

fun FabricTarget.breaks(action: Action<Dependency>) {
    breaksMap.put(this, this.project.objects.new<Dependency>().apply {
        action.execute(this)
    })
}

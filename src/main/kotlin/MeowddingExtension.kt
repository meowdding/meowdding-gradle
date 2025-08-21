package me.owdding.gradle

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class MeowddingExtension @Inject constructor(val project: Project, objectFactor: ObjectFactory) {

    val defaultLogLevel: Property<String> = objectFactor.property<String>().convention("info")
    val projectName: Property<String> = objectFactor.property<String>().convention(project.name)
    val generatedPackage: Property<String> = objectFactor.property<String>().convention(projectName.map { "me.owdding.${it.lowercase()}.generated" })

    val configureDetekt: Property<Boolean> = objectFactor.property<Boolean>().convention(false)
    internal val setupClocheClasspathFix: Property<Boolean> = objectFactor.property<Boolean>().convention(false)

    val disableJarInJarModifier: Property<Boolean> = objectFactor.property<Boolean>().convention(false)
    val disableBuildOverwrites: Property<Boolean> = objectFactor.property<Boolean>().convention(false)
    val disableWorkflowSetupTask: Property<Boolean> = objectFactor.property<Boolean>().convention(false)

    val configureModules: Property<Boolean> = objectFactor.property<Boolean>().convention(false)
    val configureCodecs: Property<Boolean> = objectFactor.property<Boolean>().convention(false)

    val moduleVersion: Property<String> = objectFactor.property<String>().convention("1.0.8")
    val codecVersion: Property<String> = objectFactor.property<String>().convention("1.0.28")

    val setupPostProcessingTasksForDatagen: Property<Boolean> = objectFactor.property<Boolean>().convention(true)

    val translationRelocation: Property<Boolean> = objectFactor.property<Boolean>().convention(true)
    val modifyShaderImports: Property<Boolean> = objectFactor.property<Boolean>().convention(true)

    fun setupClocheClasspathFix() {
        setupClocheClasspathFix.set(true)
    }

    internal fun codecDependency() = "me.owdding.ktcodecs:KtCodecs:${codecVersion.get()}"
    internal fun moduleDependency() = "me.owdding.ktmodules:KtModules:${moduleVersion.get()}"

}

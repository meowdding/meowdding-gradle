package me.owdding.gradle

import com.google.devtools.ksp.gradle.KspExtension
import earth.terrarium.cloche.ClocheExtension
import net.msrandom.minecraftcodev.core.utils.toPath
import net.msrandom.minecraftcodev.fabric.task.JarInJar
import net.msrandom.minecraftcodev.runs.task.WriteClasspathFile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.PluginAware
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import kotlin.io.path.*

class MeowddingGradlePlugin<Target : PluginAware> : Plugin<Target> {
    override fun apply(target: Target) {
        if (target !is Project) return
        val configTask = target.tasks.register<ExtractLoggerConfigTask>("extractLoggerConfig") withGroup "meowdding"
        val setupWorkflowTasks = target.tasks.register("setupForWorkflows") withGroup "meowdding"
        val releaseTask = target.tasks.register("release") withGroup "meowdding"
        val cleanReleaseTask = target.tasks.register("cleanRelease") withGroup "meowdding"
        cleanReleaseTask.configure {
            it.dependsOn(target.tasks.named("clean"))
            it.dependsOn(releaseTask)
        }

        val meowdding = target.extensions.create<MeowddingExtension>("meowdding")
        target.afterEvaluate {
            if (meowdding.disableBuildOverwrites.get()) {
                releaseTask.configure { it.enabled = false }
                cleanReleaseTask.configure { it.enabled = false }
            }

            if (meowdding.disableWorkflowSetupTask.get()) {
                setupWorkflowTasks.configure { it.enabled = false }
            }

            if (meowdding.configureDetekt.getOrElse(false)) {
                DetektIsolation.configure(target)
            }

            if (meowdding.setupClocheClasspathFix.get()) {
                target.tasks.withType<WriteClasspathFile> {
                    actions.clear()
                    actions.add {
                        output.get().toPath().also { it.parent.createDirectories() }.takeUnless { it.exists() }?.createFile()
                        generate()
                        val file = output.get().toPath()
                        file.writeText(file.readText().lines().joinToString(File.pathSeparator))
                    }
                }
            }

            target.tasks.withType<ProcessResources> {
                if (meowdding.modifyShaderImports.get()) {
                    filesMatching(listOf("**/*.fsh", "**/*.vsh")) {
                        it.filter { if (it.startsWith("//!moj_import")) "#${it.substring(3)}" else it }
                    }
                }

                if (meowdding.translationRelocation.get()) {
                    with(
                        target.copySpec {
                            it.from("src/lang").include("*.json").into("assets/skyocean/lang")
                        },
                    )
                }
                exclude(".cache/**")
            }

            if (!meowdding.disableJarInJarModifier.get()) {
                target.tasks.withType<JarInJar> {
                    include { !it.name.endsWith("-dev.jar") }
                    archiveBaseName.set(meowdding.projectName)

                    manifest.apply {
                        attributes["Fabric-Loom-Mixin-Remap-Type"] = "static"
                        attributes["Fabric-Jar-Type"] = "classes"
                        attributes["Fabric-Mapping-Namespace"] = "intermediary"
                    }
                }
            }

            target.extensions.findByType<KspExtension> {
                val sourceSets = target.extensions.getByType<SourceSetContainer>()
                sourceSets.filterNot { it.name == SourceSet.MAIN_SOURCE_SET_NAME }.forEach {
                    this.excludedSources.from(it.extensions.named<SourceDirectorySet>("kotlin").srcDirs)
                }

                arg("meowdding.project_name", meowdding.projectName.get())
                arg("meowdding.package", meowdding.generatedPackage.get())
            }

            if (meowdding.configureModules.get()) {
                target.dependencies.add("ksp", meowdding.moduleDependency())
                target.dependencies.add("compileOnly", meowdding.moduleDependency())
            }
            if (meowdding.configureCodecs.get()) {
                target.dependencies.add("compileOnly", meowdding.codecDependency())
                target.dependencies.add("ksp", meowdding.codecDependency())
            }

            val cloche = target.extensions.findByType<ClocheExtension>() ?: return@afterEvaluate
            cloche.targets.forEach { mcTarget ->
                val name = mcTarget.sourceSet.name
                target.tasks.findByPath(":${name}IncludeJar")?.let { task ->
                    releaseTask.configure {
                        it.dependsOn(task)
                        it.mustRunAfter(task)
                    }
                }

                listOf("remap${name}CommonMinecraftNamed", "remap${name}ClientMinecraftNamed").mapNotNull { target.tasks.findByPath(":$it") }.forEach { task ->
                    setupWorkflowTasks.configure {
                        it.dependsOn(task)
                        it.mustRunAfter(task)
                    }
                }

                RunConfigurator.handle(mcTarget, configTask, cloche, meowdding)
            }
        }
    }
}

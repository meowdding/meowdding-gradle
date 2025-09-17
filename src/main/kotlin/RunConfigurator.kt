@file:Suppress("UnstableApiUsage")

package me.owdding.gradle

import earth.terrarium.cloche.ClocheExtension
import earth.terrarium.cloche.api.LazyConfigurable
import earth.terrarium.cloche.api.run.RunConfigurations
import earth.terrarium.cloche.api.target.MinecraftTarget
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.put
import net.msrandom.minecraftcodev.core.MinecraftCodevPlugin.Companion.json
import net.msrandom.minecraftcodev.core.utils.getAsPath
import net.msrandom.minecraftcodev.core.utils.lowerCamelCaseGradleName
import net.msrandom.minecraftcodev.core.utils.toPath
import net.msrandom.minecraftcodev.core.utils.zipFileSystem
import net.msrandom.minecraftcodev.includes.IncludesJar
import net.msrandom.minecraftcodev.runs.MinecraftRunConfiguration
import org.gradle.api.Action
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

internal object RunConfigurator {
    fun handle(
        target: MinecraftTarget,
        configTask: TaskProvider<ExtractLoggerConfigTask>,
        cloche: ClocheExtension,
        meowdding: MeowddingExtension,
    ) {
        val project = configTask.get().project

        if (meowdding.hasAccessWideners.get()) {
            project.tasks.getByName<Jar>(lowerCamelCaseGradleName(target.sourceSet.takeUnless(SourceSet::isMain)?.name, "jar")).apply {
                doLast {
                    zipFileSystem(archiveFile.get().toPath()).use { fileSystem ->

                        val modJson = fileSystem.getPath("fabric.mod.json")

                        val inputJson = modJson.inputStream().use {
                            json.decodeFromStream<JsonObject>(it)
                        }

                        val updatedMetadata = buildJsonObject {
                            inputJson.forEach { (key, value) -> put(key, value) }

                            put("accesswidener", cloche.metadata.modId.map { "$it.accessWidener" }.get())
                        }

                        modJson.outputStream().use {
                            json.encodeToStream(updatedMetadata, it)
                        }
                    }
                }
            }
        }

        val collector = RunCollector()
        target.dependencies {
            it.localRuntime.add("net.minecrell:terminalconsoleappender:1.3.0")
            if (meowdding.configureModules.get()) it.compileOnly.add(meowdding.moduleDependency())
            if (meowdding.configureCodecs.get()) it.compileOnly.add(meowdding.codecDependency())
        }
        target.runs(collector)
        collector.forEach { (type, run) ->
            if (type.environment == Environment.CLIENT) {
                run.jvmArguments("-Ddevauth.enabled=${!(type.isDataGen || type.isTest)}")

                if (type.isDataGen) {
                    run.mainClass("net.fabricmc.loader.impl.launch.knot.KnotClient")
                }
            }

            run.prepareTask.get().apply { dependsOn(configTask) }
            configTask.get().apply {
                run.jvmArguments("-Dlog4j.configurationFile=${outputFile.getAsPath().toAbsolutePath()}")
            }

            run.jvmArguments(
                "-Dmeowdding.project=${meowdding.projectName.get()}",
                "-Dmeowdding.logLevel=${meowdding.defaultLogLevel.get()}",
            )

            if (type.isDataGen && meowdding.setupPostProcessingTasksForDatagen.get()) {
                val processResources = project.tasks.getByName<ProcessResources>(target.sourceSet.processResourcesTaskName)
                val postProcessResources = project.tasks.register<ProcessResources>(lowerCamelCaseGradleName("postProcess", run.sourceSet.get().name, "client".takeIf { type.environment == Environment.CLIENT }, "resources")) {
                    dependsOn(processResources)
                    mustRunAfter(run.runTask)
                    inputs.files(processResources.inputs.files)
                    actions.addAll(processResources.actions)
                    outputs.upToDateWhen { false }
                    destinationDir = processResources.destinationDir
                    with(processResources.rootSpec)
                }

                project.tasks.getByName(lowerCamelCaseGradleName(target.sourceSet.jarTaskName)) {
                    it.dependsOn(run.runTask)
                    it.dependsOn(postProcessResources)
                }
            }
        }
    }
}

class RunCollector : Action<RunConfigurations>, MutableMap<RunType, MinecraftRunConfiguration> by mutableMapOf() {

    private fun add(type: RunType, run: LazyConfigurable<MinecraftRunConfiguration>) {
        if (run.value.isPresent) {
            run.configure { run -> this[type] = run }
        }
    }

    override fun execute(configurations: RunConfigurations) {
        add(RunType.SERVER, configurations.server)
        add(RunType.DATA, configurations.data)
        add(RunType.TEST, configurations.test)
        add(RunType.CLIENT, configurations.client)
        add(RunType.CLIENT_DATA, configurations.clientData)
        add(RunType.CLIENT_TEST, configurations.clientTest)
    }
}

internal enum class Environment {
    SERVER,
    CLIENT,
    ;
}

internal enum class RunType(
    val environment: Environment = Environment.SERVER,
    val isDataGen: Boolean = false,
    val isTest: Boolean = false,
) {
    SERVER,
    DATA(isDataGen = true),
    TEST(isTest = true),
    CLIENT(environment = Environment.CLIENT),
    CLIENT_DATA(environment = Environment.CLIENT, isDataGen = true),
    CLIENT_TEST(environment = Environment.CLIENT, isTest = true),
    ;
}

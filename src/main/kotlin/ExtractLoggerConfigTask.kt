package me.owdding.gradle

import net.msrandom.minecraftcodev.core.utils.getAsPath
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.StandardOpenOption
import javax.inject.Inject
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeBytes

abstract class ExtractLoggerConfigTask @Inject constructor() : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        outputFile.convention { project.layout.buildDirectory.file("tmp/log4j.config.xml").get().asFile }
    }

    @TaskAction
    fun run() {
        val path = outputFile.getAsPath()
        path.createParentDirectories()
        ExtractLoggerConfigTask::class.java.classLoader.getResourceAsStream("log4j.config.xml")!!.use {
            path.writeBytes(it.readAllBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }

}
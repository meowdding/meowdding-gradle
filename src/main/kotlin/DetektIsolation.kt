package me.owdding.gradle

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

object DetektIsolation {

    fun configure(target: Project) {
        target.extensions.getByType<DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(target.rootProject.layout.projectDirectory.file("detekt/detekt.yml"))
            baseline = target.file(target.layout.projectDirectory.file("detekt/baseline.xml"))
            source.setFrom(target.extensions.getByType<SourceSetContainer>().map { it.allSource })
        }

        target.tasks.withType<Detekt> {
            onlyIf {
                project.findProperty("skipDetekt") != "true"
            }
            exclude { it.file.toPath().toAbsolutePath().startsWith(project.layout.buildDirectory.toPath()) }
            outputs.cacheIf { false }
            reports {
                it.html.required.set(true)
                it.xml.required.set(true)
                it.sarif.required.set(true)
                it.md.required.set(true)
            }
        }

        target.tasks.withType<DetektCreateBaselineTask> {
            exclude { it.file.toPath().toAbsolutePath().startsWith(project.layout.buildDirectory.toPath()) }
            outputs.cacheIf { false }
            outputs.upToDateWhen { false }
        }

        target.dependencies.add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
    }

}

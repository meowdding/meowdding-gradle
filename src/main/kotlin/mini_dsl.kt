package me.owdding.gradle

import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import java.nio.file.Path

inline fun <reified T : Task> TaskContainer.withType(crossinline configuration: T.() -> Unit = {}): Unit = this.withType(T::class.java).configureEach { configuration(it) }

inline fun <reified T> ExtensionContainer.named(name: String) = getByName(name) as T
inline fun <reified T> ExtensionContainer.getByType(): T = getByType(T::class.java)
inline fun <reified T> ExtensionContainer.getByType(init: T.() -> Unit) = getByType(T::class.java).init()
inline fun <reified T> ExtensionContainer.findByType(init: T.() -> Unit) = findByType(T::class.java)?.init()
inline fun <reified T> ExtensionContainer.findByType(): T? = findByType(T::class.java)
inline fun <reified T : Task> TaskContainer.register(name: String, noinline config: T.() -> Unit = {}): TaskProvider<T> = register(name, T::class.java, config)

inline fun <reified T> ObjectFactory.property(): Property<T> = property(T::class.java).apply { finalizeValueOnRead() }
inline fun <reified T> ExtensionContainer.create(name: String, vararg args: Any): T = this.create(name, T::class.java, *args)
inline fun <reified T : Task> TaskContainer.getByName(name: String) = this.getByName(name) as T

infix fun <T : Task> TaskProvider<T>.withGroup(group: String) = this.apply { configure { it.group = group } }

fun Provider<Directory>.toPath(): Path = get().asFile.toPath()

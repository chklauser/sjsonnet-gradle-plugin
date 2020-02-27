package com.github.chklauser.sjsonnet.gradle

import fastparse.Parsed
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import os.FilePath

import sjsonnet.Expr
import sjsonnet.Interpreter
import sjsonnet.SjsonnetMain
import java.io.File
import java.nio.charset.StandardCharsets

class SjsonnetPlugin : Plugin<Project> {
    override fun apply(target: Project) {
    }
}

object Sjsonnet {
    val parseCache: ThreadLocal<scala.collection.mutable.Map<String, Parsed<scala.Tuple2<Expr, scala.collection.immutable.Map<String, Any>>>>> = ThreadLocal.withInitial {
        SjsonnetMain.createParseCache()
    }
}

@Suppress("UnstableApiUsage")
class SjsonnetTask : DefaultTask() {

    @get:OutputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val destination: RegularFileProperty = project.objects.fileProperty()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val source: RegularFileProperty = project.objects.fileProperty()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val imports: ConfigurableFileCollection = project.objects.fileCollection()

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val searchPath: ConfigurableFileCollection = project.objects.fileCollection()


    @get:Input
    @Suppress("UNCHECKED_CAST")
    val topLevelArguments: Property<MutableMap<String, Any?>> = project.objects.property(java.util.Map::class.java) as Property<MutableMap<String, Any?>>

    @get:Input
    @Suppress("UNCHECKED_CAST")
    val externalVariables: Property<MutableMap<String, Any?>> = project.objects.property(java.util.Map::class.java) as Property<MutableMap<String, Any?>>

    @get:Input
    val indent: Property<Int> = project.objects.property(Int::class.java)

    private fun filePath(file: File): FilePath {
        return os.`FilePath$`.`MODULE$`.apply(file, os.PathConvertible.`JavaIoFileConvertible$`.`MODULE$`) as FilePath
    }

    private fun path(file: File): sjsonnet.Path {
        return sjsonnet.OsPath.apply(os.Path.apply(file, os.PathConvertible.`JavaIoFileConvertible$`.`MODULE$`))
    }

    private fun osPath(file: File): os.Path {
        return os.Path.apply(file, os.PathConvertible.`JavaIoFileConvertible$`.`MODULE$`)
    }

    private fun javaToJson(path: String, source: Any?): ujson.Value {
        return when(source) {
            null -> ujson.`Null$`.`MODULE$` as ujson.Value
            true -> ujson.`True$`.`MODULE$`
            false -> ujson.`False$`.`MODULE$`
            is Long -> ujson.`Num$`.`MODULE$`.apply(source)
            is Int -> ujson.`Num$`.`MODULE$`.apply(source)
            is Short -> ujson.`Num$`.`MODULE$`.apply(source)
            is Byte -> ujson.`Num$`.`MODULE$`.apply(source)
            is Double -> ujson.`Num$`.`MODULE$`.apply(source)
            is Float -> ujson.`Num$`.`MODULE$`.apply(source)
            is String -> ujson.`Str$`.`MODULE$`.apply(source)
            is Map<*,*> -> {
                val map = scala.collection.mutable.LinkedHashMap<String, ujson.Value>()
                for (entry in source) {
                    map.addOne(scala.Tuple2.apply(entry.key as String, javaToJson("${path}.${entry.key}", entry.value)))
                }
                ujson.`Obj$`.`MODULE$`.apply(map)
            }
            is Iterable<*> -> {
                ujson.`Arr$`.`MODULE$`.apply(toSeq(source.map { javaToJson("$path[]", it) }))
            }
            else -> throw GradleException("Cannot translate ${path}=${source} into a JSON value to pass to Jsonnet.")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun externalMap(source: MutableMap<String, Any?>): scala.collection.immutable.Map<String, ujson.Value> {
        val entries = source.mapValues { javaToJson("", it) }.entries.map { scala.Tuple2.apply(it.key, it.value) }
        // Why we need to perform a cast to scala.collection.immutable.Map<String, ujson.Value> is not clear.
        // The IntelliJ-Kotlin type checker (correctly) accepts this code, but the batch compiler infers "Any!" for the
        // `apply` method invocation.
        return scala.collection.immutable.`Map$`.`MODULE$`.apply(toSeq(entries)) as scala.collection.immutable.Map<String, ujson.Value>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T>toSeq(source: Iterable<T>): scala.collection.immutable.Seq<T> {
        return scala.collection.immutable.`Seq$`.`MODULE$`.from(scala.jdk.CollectionConverters.IterableHasAsScala(source).asScala()) as scala.collection.immutable.Seq<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T>toSet(source: Iterable<T>): scala.collection.immutable.Set<T> {
        return scala.collection.immutable.`Set$`.`MODULE$`.from(scala.jdk.CollectionConverters.IterableHasAsScala(source).asScala())
    }

    private fun <T>scalaSome(source: T): scala.Option<T> {
        return scala.`Option$`.`MODULE$`.apply(source)
    }

    @TaskAction
    fun generate() {
        val sourceFile = source.get().asFile
        val wd = sjsonnet.OsPath.apply(os.`Path$`.`MODULE$`.apply(filePath(sourceFile), osPath(sourceFile)))
        val resolver = scala.Function2 { a: sjsonnet.Path, b: String ->
            SjsonnetMain.resolveImport(toSeq(searchPath.files.map { path(it) }), scalaSome(toSet(imports.files.map{ osPath(it) })), a, b).map { scala.Tuple2(it._1() as sjsonnet.Path, it._2()) }
        }
        val i = Interpreter(Sjsonnet.parseCache.get(), externalMap(externalVariables.get()), externalMap(topLevelArguments.get()), wd, resolver)
        val resultValue = when(val result = i.interpret0(sourceFile.readText(StandardCharsets.UTF_8), path(sourceFile), sjsonnet.Renderer(java.io.StringWriter(), indent.get()))) {
            is scala.util.Left -> throw GradleException("Error while executing Jsonnet file $sourceFile: ${result.value()}")
            is scala.util.Right -> result.value()
            else -> throw GradleException("Unexpected Jsonnet interpret outcome: $result")
        }
        val destinationFile = destination.asFile.get()
        if(!destinationFile.parentFile.mkdirs()) {
            throw GradleException("Cannot create directory for jsonnet output file $destinationFile")
        }
        destinationFile.writeText(resultValue.toString(), StandardCharsets.UTF_8)
    }
}
package com.github.chklauser.sjsonnet.gradle

import java.io.{BufferedOutputStream, File, FileOutputStream, OutputStreamWriter, StringWriter}
import java.nio.charset.StandardCharsets

import org.gradle.api.file.{ConfigurableFileCollection, RegularFileProperty}
import org.gradle.api.provider.{MapProperty, Property}
import org.gradle.api.tasks.{Input, InputFile, OutputFile, PathSensitive, PathSensitivity, TaskAction}
import org.gradle.api.{DefaultTask, GradleException}
import sjsonnet.SjsonnetMain

import scala.annotation.meta.beanGetter
import scala.beans.BeanProperty
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.{Codec, Source}
import scala.util.Using

//noinspection UnstableApiUsage
class SjsonnetTask extends DefaultTask() {
  //noinspection DuplicatedCode
  @BeanProperty
  @(OutputFile@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val destination: RegularFileProperty = getProject.getObjects.fileProperty()

  //noinspection DuplicatedCode
  @BeanProperty
  @(InputFile@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val source: RegularFileProperty = getProject.getObjects.fileProperty()

  //noinspection DuplicatedCode
  @BeanProperty
  @(InputFile@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val imports: ConfigurableFileCollection = getProject.getObjects.fileCollection()

  //noinspection DuplicatedCode
  @BeanProperty
  @(InputFile@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val searchPath: ConfigurableFileCollection = getProject.getObjects.fileCollection()

  //noinspection DuplicatedCode
  @BeanProperty
  @(Input@beanGetter)
  val topLevelArguments: MapProperty[String, Any] =
  getProject.getObjects.mapProperty(classOf[String], classOf[Any])

  //noinspection DuplicatedCode
  @BeanProperty
  @(Input@beanGetter)
  val externalVariables: MapProperty[String, Any] =
  getProject.getObjects.mapProperty(classOf[String], classOf[Any])

  @BeanProperty
  @(Input@beanGetter)
  val indent: Property[Int] = getProject.getObjects.property(classOf[Int]).value(-1)

  private def javaToJson(path: String, source: Any): ujson.Value = source match {
    case null => ujson.Null
    case true => ujson.True
    case false => ujson.False
    case n: Long => ujson.Num(n)
    case n: Int => ujson.Num(n)
    case n: Short => ujson.Num(n)
    case n: Byte => ujson.Num(n)
    case n: Double => ujson.Num(n)
    case n: Float => ujson.Num(n)
    case s: CharSequence => ujson.Str(s.toString)
    case xs: java.util.Map[_, _] =>
      import _root_.scala.jdk.CollectionConverters._
      ujson.Obj(mutable.LinkedHashMap.from(xs.entrySet().asScala.map(entry =>
        (entry.getKey.asInstanceOf[String], javaToJson(s"$path.${entry.getKey}", entry.getValue))
      )))
    case xs: java.lang.Iterable[_] =>
      import scala.jdk.CollectionConverters._
      ujson.Arr(ArrayBuffer.from(xs.asScala.map(javaToJson(s"$path[]", _))))
    case value => throw new GradleException(s"Cannot translate $path=$value into a JSON value to pass to Jsonnet.")
  }

  private def externalMap(source: java.util.Map[String, Any]): Map[String, ujson.Value] = {
    import scala.jdk.CollectionConverters._
    source.asScala.toMap.map(entry => (entry._1, javaToJson("", entry._2)))
  }

  @TaskAction
  def generate(): Unit = {
    import scala.jdk.CollectionConverters._
    val sourceFile = source.get().getAsFile
    val wd = sjsonnet.OsPath(os.Path(sourceFile, os.Path(sourceFile)))
    val searchRoot = searchPath.getFiles.asScala.view.map(f => sjsonnet.OsPath(os.Path(f))).toSeq
    val knownImports = imports.getFiles.asScala.view.map(f => os.Path(f)).toSet
    val resolver = SjsonnetMain.resolveImport(searchRoot, Some(knownImports)) _
    val i = new sjsonnet.Interpreter(
      SjsonnetPlugin.parseCache.get(),
      externalMap(externalVariables.get()),
      externalMap(topLevelArguments.get()),
      wd,
      resolver)
    val destinationFile = destination.getAsFile.get()
    if (!destinationFile.getParentFile.mkdirs()) {
      throw new GradleException(s"Cannot create directory for jsonnet output file $destinationFile")
    }

    val result = Using.resources(Source.fromFile(sourceFile)(Codec.UTF8), new StringWriter()) { (source, sink) =>
      val renderer = new sjsonnet.Renderer(sink, indent.get())
      i.interpret0(source.mkString, sjsonnet.OsPath(os.Path(sourceFile)), renderer)
    }

    result match {
      case Left(error) => throw new GradleException(s"Failed to evaluate Jsonnet file $sourceFile. Error: $error")
      case Right(buf) =>
        Using(bufferedUtf8OutputStreamWriter(destinationFile)) { sink =>
          sink.write(buf.toString)
        }
    }
  }

  private def bufferedUtf8OutputStreamWriter(destinationFile: File) = {
    new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(destinationFile)), StandardCharsets.UTF_8)
  }
}

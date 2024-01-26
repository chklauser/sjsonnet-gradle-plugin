package io.github.chklauser.sjsonnet.gradle

import org.gradle.api.file.{ConfigurableFileCollection, DirectoryProperty, FileVisitDetails, RelativePath}
import org.gradle.api.provider.{MapProperty, Property}
import org.gradle.api.tasks.{CacheableTask, Input, InputFiles, OutputDirectory, PathSensitive, PathSensitivity, SkipWhenEmpty, TaskAction}
import org.gradle.api.{DefaultTask, GradleException}
import os.Path
import sjsonnet.Importer
import sjsonnet.ResolvedFile
import sjsonnet.{OsPath, SjsonnetMain}

import java.io.{BufferedOutputStream, File, FileOutputStream, OutputStreamWriter, StringWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.annotation.meta.beanGetter
import scala.beans.BeanProperty
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.{Codec, Source}
import scala.util.Using

@CacheableTask
//noinspection UnstableApiUsage
class SjsonnetTask extends DefaultTask() {

  //noinspection DuplicatedCode
  @BeanProperty
  @(OutputDirectory@beanGetter)
  val outputDirectory: DirectoryProperty = getProject.getObjects.directoryProperty()

  //noinspection DuplicatedCode
  @BeanProperty
  @(SkipWhenEmpty@beanGetter)
  @(InputFiles@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val sources: ConfigurableFileCollection = getProject.getObjects.fileCollection()

  //noinspection DuplicatedCode
  @BeanProperty
  @(SkipWhenEmpty@beanGetter)
  @(InputFiles@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val additionalInputs: ConfigurableFileCollection = getProject.getObjects.fileCollection()

  //noinspection DuplicatedCode
  @BeanProperty
  @(InputFiles@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val imports: ConfigurableFileCollection = getProject.getObjects.fileCollection()

  //noinspection DuplicatedCode
  @BeanProperty
  @(InputFiles@beanGetter)
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

  //noinspection DuplicatedCode
  @BeanProperty
  @(Input@beanGetter)
  val indent: Property[Int] = getProject.getObjects.property(classOf[Int]).convention(2)

  private def javaToJson(path: String, source: Any): ujson.Value = {
    getLogger.info("javaToJson({}, {})", Array(path, source):_*) ; source match {
    case null => ujson.Null
    case true => ujson.True
    case false => ujson.False
    case n: Long => ujson.Num(n.toDouble)
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
  }}

  private def externalMap(source: java.util.Map[String, Any]): Map[String, String] = {
    import scala.jdk.CollectionConverters._
    source.asScala.toMap.map(entry => (entry._1, ujson.write(javaToJson("", entry._2))))
  }

  @TaskAction
  def generate(): Unit = {
    import scala.jdk.CollectionConverters._
    val searchRoot = searchPath.getFiles.asScala.view.map(f => sjsonnet.OsPath(os.Path(f))).toSeq
    val knownImports = imports.getFiles.asScala.view.map(f => os.Path(f)).toSet
    sources.getAsFileTree.visit((v: FileVisitDetails) => {
      if (!v.isDirectory) {
        generateFromSource(v.getFile, v.getRelativePath.getParent, sjsonnetInterpreter(_, searchRoot, knownImports))
      }
    })
  }

  private def outputFileName(sourceFile: File): String = {
    sourceFile.getName.replaceFirst("\\.[^.]+$", ".json")
  }

  private def generateFromSource(sourceFile: File, relativeOutputDir: RelativePath,
                                 interpreterFor: File => sjsonnet.Interpreter): Unit = {
    // Prepare destination
    val nestedOutputDirectory = if(relativeOutputDir.length() > 0) {
      outputDirectory.dir(relativeOutputDir.getPathString).get()
    } else {
      outputDirectory.get()
    }
    val destinationFile = nestedOutputDirectory.file(outputFileName(sourceFile)).getAsFile
    getLogger.debug("Generate {} into {}", Array(sourceFile.getPath, destinationFile.getPath):_*)
    Files.createDirectories(nestedOutputDirectory.getAsFile.toPath)

    // TODO: support multiple output files from one source

    // Render Jsonnet into in-memory buffer
    val result = Using.resources(Source.fromFile(sourceFile)(Codec.UTF8), new StringWriter()) { (source, sink) =>
      val renderer = new sjsonnet.Renderer(sink, indent.get())
      interpreterFor(sourceFile).interpret0(source.mkString, sjsonnet.OsPath(os.Path(sourceFile)), renderer)
    }

    // Write in-memory buffer to destination
    result match {
      case Left(error) => throw new GradleException(s"Failed to evaluate Jsonnet file $sourceFile. Error: $error")
      case Right(buf) =>
        Using(bufferedUtf8OutputStreamWriter(destinationFile)) { sink =>
          sink.write(buf.toString)
        }
    }
    ()
  }

  /**
   * Initialize an sjsonnet interpreter using the shared parser cache.
   *
   * @param sourceFile The source file (determines working directory)
   * @return The interpreter
   */
  private def sjsonnetInterpreter(sourceFile: File,
                                 searchRoot: Seq[OsPath], knownImports: Set[Path]): sjsonnet.Interpreter = {
    val workingDirectory = sjsonnet.OsPath(os.Path(sourceFile, os.Path(sourceFile)))

    val allowedImports = if(knownImports.nonEmpty) { Some(knownImports) } else { None }
    val importer = SjsonnetMain.resolveImport(searchRoot, allowedImports)
    val importerWithLogging = new Importer {
      override def resolve(docBase: sjsonnet.Path, importName: String): Option[sjsonnet.Path] = {
        val result = importer.resolve(docBase, importName)
        if(getLogger.isDebugEnabled) {
          getLogger.debug("jsonnet import working_directory={}, import={} ==> {}", Array(
            docBase, importName, result
          ): _*)
        }
        result
      }

      override def read(path: sjsonnet.Path): Option[ResolvedFile] = importer.read(path)
    }

    new sjsonnet.Interpreter(
      externalMap(externalVariables.get()),
      externalMap(topLevelArguments.get()),
      workingDirectory,
      importerWithLogging,
      SjsonnetPlugin.parseCache.get())
  }

  private def bufferedUtf8OutputStreamWriter(destinationFile: File) = {
    new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(destinationFile)), StandardCharsets.UTF_8)
  }
}

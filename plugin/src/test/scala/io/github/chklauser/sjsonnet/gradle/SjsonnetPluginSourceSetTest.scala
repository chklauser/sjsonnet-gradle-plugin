package io.github.chklauser.sjsonnet.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.{BeforeEach, Test}
import org.scalatest.Assertions

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

class SjsonnetPluginSourceSetTest extends SjsonnetPluginTestSupport with Assertions {
  var project : Project = _
  var directory : Path = _

  @BeforeEach
  def beforeEach(@TempDir tempDir: Path): Unit = {
    this.directory = tempDir
    project = ProjectBuilder.builder().withProjectDir(tempDir.toFile).build()
  }

  @Test
  def workWithoutJavaPlugin(): Unit = {
    applySjsonnetPlugin()
    sjsonnetExt.create("t1")
    assert(project.getTasks.findByName("jsonnetT1Generate") != null)
  }

  @Test
  def workWithDefaultSourceSets(): Unit = {
    applyJavaPlugin()
    applySjsonnetPlugin()
    sjsonnetExt.create("t2")
    assert(project.getTasks.findByName("jsonnetT2Generate") != null)
  }

  @Test
  def workWithDefaultSourceSetsInvertedOrder(): Unit = {
    // apply sjsonnet _before_ java plugin
    applySjsonnetPlugin()
    applyJavaPlugin()
    sjsonnetExt.create("t2")
    assert(project.getTasks.findByName("jsonnetT2Generate") != null)
  }

  @Test
  def assumeSourceSetPathAsDefault(): Unit = {
    // GIVEN
    // prepare source files
    val f1 = directory.resolve("src/main/jsonnet/f1.jsonnet")
    val f2 = directory.resolve("src/main/jsonnet/subdir/f2.jsonnet")
    Files.createDirectories(f1.getParent)
    Files.createDirectories(f2.getParent)
    Files.writeString(f1, "")
    Files.writeString(f2, "")

    // setup project
    applyJavaPlugin()
    applySjsonnetPlugin()

    // WHEN
    // Name matches an existing source set.
    sjsonnetExt.create("main")

    // THEN
    val rawTask = project.getTasks.findByName("jsonnetMainGenerate")
    assert(rawTask != null)
    assert(rawTask.isInstanceOf[SjsonnetTask])
    val task = rawTask.asInstanceOf[SjsonnetTask]
    assert(task.sources.getFiles.asScala == Set(f1.toFile, f2.toFile))

    // check that output dir is included in resources
    assert(sourceSets.getByName("main").getResources.getSrcDirs.asScala.map(_.toPath)
        .contains(task.getOutputDirectory().get.getAsFile.toPath))
  }

  @Test
  def assumeSourceSetPathAsDefaultInverted(): Unit = {
    // GIVEN
    // prepare source files
    val f1 = directory.resolve("src/main/jsonnet/f1.jsonnet")
    val f2 = directory.resolve("src/main/jsonnet/subdir/f2.jsonnet")
    Files.createDirectories(f1.getParent)
    Files.createDirectories(f2.getParent)
    Files.writeString(f1, "")
    Files.writeString(f2, "")

    // setup project (apply sjsonnet before java)
    applySjsonnetPlugin()
    applyJavaPlugin()

    // WHEN
    // Name matches an existing source set.
    sjsonnetExt.create("main")

    // THEN
    val rawTask = project.getTasks.findByName("jsonnetMainGenerate")
    assert(rawTask != null)
    assert(rawTask.isInstanceOf[SjsonnetTask])
    val task = rawTask.asInstanceOf[SjsonnetTask]
    assert(task.sources.getFiles.asScala == Set(f1.toFile, f2.toFile))

    // check that output dir is included in resources
    assert(sourceSets.getByName("main").getResources.getSrcDirs.asScala.map(_.toPath)
        .contains(task.getOutputDirectory().get.getAsFile.toPath))
  }

}

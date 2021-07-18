package io.github.chklauser.sjsonnet.gradle

import org.gradle.api.tasks.TaskProvider
import org.gradle.api.{Action, Project, Task}
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.{BeforeEach, Test}
import org.junit.jupiter.api.io.TempDir
import org.scalatest.Assertions

import java.nio.file.Path
import scala.jdk.CollectionConverters._

class SjsonnetPluginTest extends  SjsonnetPluginTestSupport with Assertions {
  var project : Project = _
  var task : SjsonnetTask = _

  @Test
  def provideDefaultsFromSpec(): Unit = {
    assert(task.getName == "jsonnetSjsTestGenerate")

    assert(task.outputDirectory.get().getAsFile.toPath.endsWith("build/out"))

    val sources = task.sources.asScala.map(_.toPath).toSeq
    assert(sources.exists(_.endsWith("src/jsonnet/f1.jsonnet")))
    assert(sources.exists(_.endsWith("src/jsonnet/f2.jsonnet")))
    assert(sources.size == 2)

    val imports = task.imports.asScala.map(_.toPath).toSeq
    assert(imports.exists(_.endsWith("src/jsonnet/lib1.jsonnet")))
    assert(imports.exists(_.endsWith("src/jsonnet/lib1.jsonnet")))
    assert(imports.size == 2)

    val searchPath = task.searchPath.asScala.map(_.toPath).toSeq
    assert(searchPath.exists(_.endsWith("lib/1")))
    assert(searchPath.exists(_.endsWith("lib/2")))
    assert(searchPath.size == 2)

    val topLevelArguments = task.topLevelArguments.get().asScala
    assert(topLevelArguments == Map(
      "a1" -> 2,
      "a2" -> false,
      "a3" -> "text3",
      "a4" -> Seq(true, "4", 4).asJava,
      "a5" -> Map("k1" -> 1.2, "k2" -> "text52").asJava
    ))

    val externalVariables = task.externalVariables.get().asScala
    assert(externalVariables == Map(
      "e1" -> 1,
      "e2" -> true,
      "e3" -> "str3",
      "e4" -> Seq(1, 2, 3).asJava,
      "e5" -> Map("k1" -> true, "k2" -> "str52").asJava
    ))

    assert(task.indent.get() == 3)
  }

  @Test
  def aggregateTheTask(): Unit =  {
    val aggregate = project.getTasks.findByName("jsonnetGenerate")
    assert(aggregate != null)
    assert(aggregate.getDependsOn.asScala.exists(x => x match {
      case p: TaskProvider[_] => p.getName == task.getName
      case t: Task => t.getName == task.getName
      case _ => false
    }))
  }

  @BeforeEach 
  def beforeEach(@TempDir directory: Path): Unit = {
    project = ProjectBuilder.builder().withProjectDir(directory.toFile).build()
    applySjsonnetPlugin()

    sjsonnetExt.create("sjsTest", (spec => {
      spec.outputDirectory.set(directory.resolve("build/out").toFile)
      spec.sources.from(
        directory.resolve("src/jsonnet/f1.jsonnet"),
        directory.resolve("src/jsonnet/f2.jsonnet"))
      spec.imports.from(
        directory.resolve("src/jsonnet/lib1.jsonnet"),
        directory.resolve("src/jsonnet/lib2.jsonnet")
      )
      spec.searchPath.from(
        directory.resolve("lib/1"),
        directory.resolve("lib/2"),
      )

      spec.topLevelArguments.put("a1", 2)
      spec.topLevelArguments.put("a2", false)
      spec.topLevelArguments.put("a3", "text3")
      spec.topLevelArguments.put("a4", Seq(true, "4", 4).asJava)
      spec.topLevelArguments.put("a5", Map("k1" -> 1.2, "k2" -> "text52").asJava)

      spec.externalVariables.put("e1", 1)
      spec.externalVariables.put("e2", true)
      spec.externalVariables.put("e3", "str3")
      spec.externalVariables.put("e4", Seq(1, 2, 3).asJava)
      spec.externalVariables.put("e5", Map("k1" -> true, "k2" -> "str52").asJava)

      spec.indent.set(3)
    }):Action[_ >: SjsonnetSpec])

    // Extract task for test case
    val rawTask = project.getTasks.findByName("jsonnetSjsTestGenerate")
    assert(rawTask != null)
    assert(rawTask.isInstanceOf[SjsonnetTask])
    task = rawTask.asInstanceOf[SjsonnetTask]
  }
}

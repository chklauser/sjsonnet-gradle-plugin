package io.github.chklauser.sjsonnet.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.{BeforeEach, Test}
import org.scalatest.Assertions

class SjsonnetTaskTest extends Assertions {

  var project : Project = _
  var task : SjsonnetTask = _

  @Test
  def emptySourceSetNoAction(): Unit = {
    task.generate()
  }

  @BeforeEach
  def beforeEach(): Unit = {
    project = ProjectBuilder.builder().build();
    task = project.getTasks.create("jsonnet", classOf[SjsonnetTask])
  }
}

package io.github.chklauser.sjsonnet.gradle

import io.github.chklauser.sjsonnet.gradle.internal.implicits._
import org.gradle.api.file.{ConfigurableFileCollection, DirectoryProperty, SourceDirectorySet}
import org.gradle.api.provider.{MapProperty, Property}
import org.gradle.api.tasks.{Input, InputFile, InputFiles, OutputDirectories, OutputDirectory, OutputFile, PathSensitive, PathSensitivity}
import org.gradle.api.{Action, GradleException, Project}

import scala.annotation.meta.beanGetter
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

//noinspection UnstableApiUsage
class SjsonnetSpec(@BeanProperty val name : String, project: Project) {
  if(name == null || name.isEmpty) {
    throw new GradleException("Name of sjsonnet spec cannot be null or empty.")
  }

  @BeanProperty
  val namePascalCase: String = s"${name.charAt(0).toUpper}${name.substring(1)}"

  //noinspection DuplicatedCode
  @BeanProperty
  @(OutputDirectory@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val outputDirectory: DirectoryProperty = project.getObjects.directoryProperty()
  outputDirectory.convention(project.getLayout.getBuildDirectory.dir(s"generated/resources/$name/jsonnet"))

  //noinspection DuplicatedCode
  @BeanProperty
  @(InputFiles@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val sources: ConfigurableFileCollection = project.getObjects.fileCollection()

  //noinspection DuplicatedCode
  @BeanProperty
  @(InputFiles@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val additionalInputs: ConfigurableFileCollection = project.getObjects.fileCollection()

  //noinspection DuplicatedCode
  @BeanProperty
  @(InputFiles@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val imports: ConfigurableFileCollection = project.getObjects.fileCollection()

  //noinspection DuplicatedCode
  @BeanProperty
  @(InputFiles@beanGetter)
  @(PathSensitive@beanGetter)(PathSensitivity.RELATIVE)
  val searchPath: ConfigurableFileCollection = project.getObjects.fileCollection()

  //noinspection DuplicatedCode
  @BeanProperty
  @(Input@beanGetter)
  val topLevelArguments: MapProperty[String, Any] = project.getObjects.mapProperty(classOf[String], classOf[Any])

  //noinspection DuplicatedCode
  @BeanProperty
  @(Input@beanGetter)
  val externalVariables: MapProperty[String, Any] = project.getObjects.mapProperty(classOf[String], classOf[Any])

  //noinspection DuplicatedCode
  @BeanProperty
  @(Input@beanGetter)
  val indent: Property[Int] = project.getObjects.property(classOf[Int]).convention(-1)

  {
    project.sourceSets.flatMap(ss => Option(ss.findByName(name))).foreach { sourceSet =>
      // copy source set
      sourceSet.jsonnet.foreach(s => {
        sources.from(s.getJsonnet)
        additionalInputs.from(s.getJsonnet.getSrcDirs)
      })

      // include output in resources
      sourceSet.resources({ rs =>
        rs.srcDir(outputDirectory)
      }:Action[_ >: SourceDirectorySet])
    }
  }
}

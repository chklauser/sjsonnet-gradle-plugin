package io.github.chklauser.sjsonnet.gradle

import org.gradle.api.{NamedDomainObjectContainer, Project}
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer


trait SjsonnetPluginTestSupport {
  protected def project : Project

  protected def sjsonnetExt: NamedDomainObjectContainer[SjsonnetSpec] =
    project.getExtensions.getByName("sjsonnet").asInstanceOf[NamedDomainObjectContainer[SjsonnetSpec]]
  protected def sourceSets: SourceSetContainer =
    project.getProperties.get("sourceSets").asInstanceOf[SourceSetContainer]

  protected def applySjsonnetPlugin(): Unit = project.getPlugins.apply(classOf[SjsonnetPlugin])
  protected def applyJavaPlugin(): Unit = project.getPlugins.apply(classOf[JavaPlugin])
}

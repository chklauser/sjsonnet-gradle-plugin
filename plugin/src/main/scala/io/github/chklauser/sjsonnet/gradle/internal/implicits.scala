package io.github.chklauser.sjsonnet.gradle.internal

import io.github.chklauser.sjsonnet.gradle.JsonnetSourceSet
import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.Project
import org.gradle.api.plugins.Convention
import org.gradle.api.tasks.{SourceSet, SourceSetContainer}

object implicits {
  implicit class ProjectExt(target: Project) {
    def sourceSets: Option[SourceSetContainer] = target.getProperties.get("sourceSets") match {
      case sourceSets: SourceSetContainer => Some(sourceSets)
      case _ => None
    }
  }

  implicit class SourceSetExt(inner: SourceSet) {
    def jsonnet : Option[JsonnetSourceSet] = InvokerHelper.getProperty(inner, "convention") match {
      case sourceSetConvention: Convention => Option(sourceSetConvention.findPlugin(classOf[JsonnetSourceSet]))
      case _ => None
    }
  }

  implicit class SourceSetContainerExt(inner: SourceSetContainer) {
    def findByNameOpt(name: String): Option[SourceSet] = Option(inner.findByName(name))
  }
}

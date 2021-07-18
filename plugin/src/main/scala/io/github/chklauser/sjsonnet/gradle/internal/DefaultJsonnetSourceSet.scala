package io.github.chklauser.sjsonnet.gradle.internal

import groovy.lang.Closure
import io.github.chklauser.sjsonnet.gradle.JsonnetSourceSet
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.util.ConfigureUtil

class DefaultJsonnetSourceSet(name: String, displayName: String, objectFactory: ObjectFactory) extends JsonnetSourceSet {
  val jsonnet: SourceDirectorySet = objectFactory.sourceDirectorySet(name, displayName)

  override def getJsonnet: SourceDirectorySet = jsonnet

  override def jsonnet(closure: Closure[_]): JsonnetSourceSet = {
    ConfigureUtil.configure(closure, jsonnet)
    this
  }
}

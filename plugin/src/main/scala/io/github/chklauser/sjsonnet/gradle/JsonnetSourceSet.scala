package io.github.chklauser.sjsonnet.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet

trait JsonnetSourceSet {
  def getName: String = getJsonnet.getName

  def getJsonnet: SourceDirectorySet

  def jsonnet(closure: Closure[_]): JsonnetSourceSet

  def jsonnet(action: Action[SourceDirectorySet]): JsonnetSourceSet = {
    action.execute(getJsonnet)
    this
  }
}

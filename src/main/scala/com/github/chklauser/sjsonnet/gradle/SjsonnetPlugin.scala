package com.github.chklauser.sjsonnet.gradle

import fastparse.Parsed
import org.gradle.api.{Plugin, Project}
import sjsonnet.{Expr, SjsonnetMain}

import scala.collection.mutable

class SjsonnetPlugin extends Plugin[Project] {
  override def apply(target: Project): Unit = ???
}

object SjsonnetPlugin {
  val parseCache: ThreadLocal[mutable.Map[String, Parsed[(Expr, Map[String, Int])]]] =
    ThreadLocal.withInitial(() =>  SjsonnetMain.createParseCache())
}


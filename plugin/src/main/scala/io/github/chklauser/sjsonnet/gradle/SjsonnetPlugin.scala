package io.github.chklauser.sjsonnet.gradle

import fastparse.Parsed
import io.github.chklauser.sjsonnet.gradle.internal.DefaultJsonnetSourceSet
import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.file.{ConfigurableFileCollection, SourceDirectorySet}
import org.gradle.api.plugins.{Convention, JavaPlugin}
import org.gradle.api.tasks.{SourceSet, SourceSetContainer}
import org.gradle.api.{Action, Plugin, Project, ProjectEvaluationListener, ProjectState, Task}
import sjsonnet.{Expr, FileScope, Path, SjsonnetMain}

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable
import internal.implicits._
import sjsonnet.DefaultParseCache
import sjsonnet.ParseCache

class SjsonnetPlugin extends Plugin[Project] {
  private[this] var sourceSetRegistered = new AtomicBoolean(false)
  override def apply(target: Project): Unit = {
    // Aggregate task for all automatically created jsonnet task
    val aggregate = target.getTasks.register("jsonnetGenerate")

    // `sjsonnet` extension (generate tasks for each named sjsonnet block)
    val specContainer = target.container(classOf[SjsonnetSpec], (name: String) => new SjsonnetSpec(name, target))
    target.getExtensions.add("sjsonnet", specContainer)
    specContainer.all({ s: SjsonnetSpec =>
      val generate = target.getTasks.register(s"jsonnet${s.namePascalCase}Generate",
        classOf[SjsonnetTask], (t => {
          t.outputDirectory.fileProvider(s.outputDirectory.getAsFile)
          initializeFileCollection(s.imports, t.imports)
          initializeFileCollection(s.searchPath, t.searchPath)
          initializeFileCollection(s.sources, t.sources)
          initializeFileCollection(s.additionalInputs, t.additionalInputs)
          t.externalVariables.convention(s.externalVariables)
          t.topLevelArguments.convention(s.topLevelArguments)
          t.indent.convention(s.indent)
          t.preserveOrder.convention(s.preserveOrder)
          t.strict.convention(s.strict)
          t.noStaticErrors.convention(s.noStaticErrors)
          t.noDuplicateKeysInComprehension.convention(s.noDuplicateKeysInComprehension)
          t.strictImportSyntax.convention(s.strictImportSyntax)
          t.strictInheritedAssertions.convention(s.strictInheritedAssertions)
        }): Action[SjsonnetTask])
      aggregate.configure(t => t.dependsOn(generate))

      // If this specification matches a source set, register this task as part of the resource processing
      target.sourceSets.flatMap(_.findByNameOpt(s.name)).foreach(sourceSet => {
        target.getTasks.named(sourceSet.getProcessResourcesTaskName).configure({ processTask =>
          processTask.dependsOn(generate)
        }:Action[_ >: Task])
      })
    })

    target.getPlugins.withType(classOf[JavaPlugin]).all({ _: Plugin[Project] =>
      if(sourceSetRegistered.compareAndExchange(false, true)) {
        // already configured
        return
      }

      // Register 'jsonnet' as a "language" for all source sets, if sourceSets are a thing in this project
      val jsonnetSourceSetContainer = target.container(classOf[JsonnetSourceSet], (name: String) =>
        new DefaultJsonnetSourceSet(name, s"$name jsonnet sources", target.getObjects): JsonnetSourceSet)
      target.getProperties.get("sourceSets") match {
        case sourceSets: SourceSetContainer => sourceSets.all((sourceSet => {
          InvokerHelper.getProperty(sourceSet, "convention") match {
            case sourceSetConvention: Convention =>
              val jsonnetSourceSet = jsonnetSourceSetContainer.maybeCreate(sourceSet.getName)
              val srcDir = target.file(s"src/${sourceSet.getName}/jsonnet")
              jsonnetSourceSet.getJsonnet.srcDir(srcDir)
              // only include *.jsonnet files so that shared libraries (that don't produce a useful output themselves)
              // can be included as *.libjsonnet files in the same source tree.
              jsonnetSourceSet.getJsonnet.include("**/*.jsonnet")
              sourceSetConvention.getPlugins.put("jsonnet", jsonnetSourceSet)
              sourceSet.getExtensions.add(classOf[SourceDirectorySet], "jsonnet", jsonnetSourceSet.getJsonnet)
              target.getLogger.debug("jsonnet registered in source set {}. Path: {}",
                Array(sourceSet.getName, srcDir):_*)
            case prop => target.getLogger.debug("Project's {} source set doesn't have a convention to extend. Got {}",
              Array(sourceSet.getName, prop): _*)
          }
        }): Action[SourceSet])
        case other => target.getLogger.debug("Project doesn't have sourceSets to extend. Got {}",
          Array(other): _*)
      }
    }:Action[_ >: Plugin[Project]])
  }

  private def initializeFileCollection(source: ConfigurableFileCollection, dest: ConfigurableFileCollection): Unit = {
    dest.setFrom(source.getFrom)
    dest.setBuiltBy(source.getBuiltBy)
  }
}

object SjsonnetPlugin {
  val parseCache: ThreadLocal[ParseCache] =
    ThreadLocal.withInitial(() => new DefaultParseCache())
}

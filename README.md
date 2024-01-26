# sjsonnet-gradle-plugin
Gradle plugin for running sjsonnet (Scala implementation of the Jsonnet JSON templating language).

This project is not associated with [databricks/sjsonnet](https://github.com/databricks/sjsonnet), but builds on their
excellent work.

## Usage

```kotlin
// build.gradle.kts (Kotlin Script Syntax)
plugins {
    id("io.github.chklauser.sjsonnet") version "0.1.0"
}
```

The plugin offers three levels of configuration-by-convention vs. control.

### Source-Sets

If you want to use the standard source file layout and have the generated output included
in the resources of the source sets, this approach is recommended.

```text
src/
├─ main/
│  ├─ java/
│   ` jsonnet/
│     ├─ shared.libsonnet
│     ├─ application.jsonnet
│      ` application-dev.jsonnet
 ` test/
   ├─ java/
    ` jsonnet/
       ` mock-data.jsonnet
```

To keep your Gradle task list tidy, the sjsonnet-gradle-plugin only generates tasks 
for source sets that you define a specification for. In this example, we want to
use jsonnet in both `test` and the `main` source set:

```kotlin
// build.gradle.kts (Kotlin Script Syntax)

sjsonnet {
  // minimal configuration (default settings for the `main` source set)
  create("main") { }
  
  // custom settings for the `test` source set
  create("test") { 
      // setting a non-zero indentation enables pretty-printing
      indent.set(2)
  }
}
```

Custom source sets are also supported. Whenever the name of an `sjsonnet` specification matches the name
of an existing source-set, the plugin will automatically configure the specification
to use the sources from the source-set and to consider the outputs part of the 'resources'
for that source set (i.e. have the outputs included in the JAR).

In this mode, the plugin only considers files ending in `*.jsonnet` source files. The remaining
files in the directory are considered 'input files' (relevant for up-to-date checking). 
That way, you can keep shared definitions (commonly kept in `*.libsonnet` files) and
data (used with `importstr`) together with the generator entry-points.

### Custom specifications
If you don't want to work with source-sets (or don't want to include the output in resources), 
you can define custom specifications. **The name of a custom specification must not match
any of the source sets**.

In the example below, the source files will be read from the `./custom-src` directory.

```kotlin
// build.gradle.kts (Kotlin Script Syntax)

sjsonnet { 
  create("custom") {
    indent.set(4)
    sources.from(project.layout.projectDirectory.dir("custom-src"))
  }
}

tasks["assemble"].dependsOn("jsonnetCustomGenerate")
```

#### Imports
When you use `import` and `importstr` together with custom specifications, you need to take care
that Gradle knows about the files that might additionally influence the generator.

In this example, we define the sources as being _only_ the `*.jsonnet` files (via the filter)
and then, separately, add the directory as `additionalInputs`. The latter is ignored by the
plugin, but used by Gradle to decide whether the generator tasks are `UP-TO-DATE` or not.

```kotlin
// build.gradle.kts (Kotlin Script Syntax)

sjsonnet { 
  create("customWithImports") {
    indent.set(2)
    val srcDir = project.layout.projectDirectory.dir("custom-src")
    sources.from(srcDir)
    sources.filter {f -> f.extension == "jsonnet"}
    additionalInputs.from(srcDir)
  }
}

tasks["assemble"].dependsOn("jsonnetCustomWithImportsGenerate")
```

### Custom task
Instead of using the specification extension, you can manually define `SjsonnetTask`s
for full control. The task supports 1:1 the same configuration options as the specification.

```kotlin
// build.gradle.kts (Kotlin Script Syntax)

tasks {
  "generateData"(io.github.chklauser.sjsonnet.gradle.SjsonnetTask::class) {
    sources.from(project.layout.projectDirectory.dir("data"))
  }
}
```

If you use `import` or `importstr`, the same considerations as for the "custom specifications"
approach apply.

### Settings
The plugin supports a subset of the settings of sjsonnet. 
See [Config.scala](https://github.com/databricks/sjsonnet/blob/9534260fff4a50d29db379e307bce9a484790fa7/sjsonnet/src-jvm-native/sjsonnet/Config.scala)
for the documentation of those settings.

All settings below are also available on the `SjsonnetTask`.

```kotlin
// build.gradle.kts (Kotlin Script Syntax)

sjsonnet {
  create("main") {
    // Sjsonnet settings
    indent.set(2)
    preserveOrder.set(true)
    strict.set(true)
    noStaticErrors.set(true)
    noDuplicateKeysInComprehension.set(true)
    strictImportSyntax.set(true)
    strictInheritedAssertions.set(true)
    topLevelArguments.put("a1", 2)
    externalVariables.put("e1", 1)

    // Gradle plugin settings
    // `searchPath` is the list of directories to search for imported files 
    searchPath.from(...)
    // If `imports` is set, then it serves as an allow-list for imports (only files in the list can be imported)
    // This is useful because the plugin needs to list dependencies for up-to-date checking. Un-declared imported
    // files can lead to unpredictable result.
    imports.from(...)
  }
}
```

## FAQ
### Why write a Gradle plugin in scala?
The `s` in `sjsonnet` stands for "Scala". Its API is not straightforward to
use from Java or Kotlin.

### Why is the plugin/specification section/task type called "sjsonnet", but the generated tasks/source set extensions are called "jsonnet"?
"jsonnet" is the language, "sjsonnet" is the implementation. The plugin uses "sjsonnet"
and thus the task type, settings, plugin name carry that name.

But the outside world (gradle user) don't need to know about this implementation detail.
They are only concerned with having their `*.jsonnet` files processed quickly.

### Why use sjsonnet and not the official jsonnet implementation
Windows support and speed. It's not easy to get a hold of Windows builds
for the official implementation. But more importantly: the official implementation
is not very efficient. It can take _seconds_ to generate even just medium-sized
output files. jsonnet is a pure, functional language, yet the official implementation
does little to no caching of re-used expressions. Worse, it implements its entire
standard library in jsonnet. While this may be an interesting intellectual exercise,
it means that you pay for the inefficient implementation when using standard 
library functions as well.

sjsonnet addresses both issues. Being written in Scala, it runs on the JVM, inside
the Gradle process on any platform where Gradle runs. On the performance front, 
it applies caching much more aggressively and implements the
jsonnet standard library in Java.

### Why are the Gradle configuration examples in Kotlin?
Two reasons. First, static types make writing Gradle build scripts more pleasant.
Second, the Kotlin syntax is usually a bit trickier to figure out. Going from
the Kotlin syntax back to Groovy tends to be simpler.


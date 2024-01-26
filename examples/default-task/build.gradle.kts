plugins {
    java
    scala // scala is only used for testing; not part of the example
    id("io.github.chklauser.sjsonnet")
}

group = "io.github.chklauser.sjsonnet.examples"
version = "0.0.0"

sourceSets.create("mock") { }

sjsonnet {
    // This spec doesn't match the name of the source set. This means that we have to configure
    // the source directory manually.
    create("mockData") {
        indent.set(3)
        val jsonnet = sourceSets["mock"].extensions["jsonnet"] as SourceDirectorySet
        // sources should contain the *.jsonnet files to run through the generator
        sources.from(jsonnet)

        // The plugin doesn't know which additional files will get imported via `import` and `importstr`.
        // Such additional files need to be listed as additional inputs for Gradle's up-to-date checking/caching
        // to work correctly.
        // The plugin doesn't do anything with these files. They are simply listed as task inputs.
        additionalInputs.from(jsonnet.srcDirs)
    }

    create("custom") {
        indent.set(4)
        sources.from(project.layout.projectDirectory.dir("custom-src"))
        // no need to specify additional inputs because each *.jsonnet file is self-contained
    }

    create("main") {
        // No need to configure anything else.
        // It is, however, required to define the spec. Without a spec, no
        // tasks get generated.
    }
}

tasks["assemble"].dependsOn("jsonnetMockDataGenerate")

// Used for testing (not part of the example)

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.12")
    testImplementation("org.scalatest:scalatest_2.13:3.2.17")
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
    "test"(Test::class) {
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        useJUnitPlatform {
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
        dependsOn("jsonnetGenerate")
    }
}

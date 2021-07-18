plugins {
    java
    scala
    `java-gradle-plugin`
}

group = "io.github.chklauser.sjsonnet"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.6")
    implementation("com.databricks:sjsonnet_2.13:0.4.0")
    testImplementation("org.scalatest:scalatest_2.13:3.2.9")
    testImplementation(platform("org.junit:junit-bom:5.7.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

gradlePlugin {
    plugins {
        create("jsonnet") {
            id = "io.github.chklauser.sjsonnet"
            displayName = "sjsonnet"
            description = "Plugin for running sjsonnet (Scala implementation of the Jsonnet JSON templating language)"
            implementationClass = "io.github.chklauser.sjsonnet.gradle.SjsonnetPlugin"
        }
    }
}

tasks {
    "test"(Test::class) {
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        useJUnitPlatform {
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }
}
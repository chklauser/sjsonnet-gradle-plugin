plugins {
    java
    scala
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.1")
    implementation("com.lihaoyi:sjsonnet_2.13:0.2.3")
    testImplementation("org.scalatest:scalatest_2.13:3.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}


gradlePlugin {
    plugins {
        create("jsonnet") {
            id = "io.github.chklauser.jsonnet"
            displayName = "Sjsonnet"
            description = "Plugin for running sjsonnet (Scala implementation of the Jsonnet JSON templating language)"
            implementationClass = "com.github.chklauser.sjsonnet.gradle.SjsonnetPlugin"
        }
    }
}

tasks {
    "test"(Test::class) {
        useJUnitPlatform()
    }
}
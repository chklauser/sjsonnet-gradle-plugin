plugins {
    kotlin("jvm") version "1.3.61"
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib", "1.3.61"))
    implementation("com.lihaoyi:sjsonnet_2.13:0.2.3")
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
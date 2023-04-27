plugins {
    java
    scala
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.0"
}

group = "io.github.chklauser.sjsonnet"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.10")
    implementation("com.databricks:sjsonnet_2.13:0.4.0")
    testImplementation("org.scalatest:scalatest_2.13:3.2.9")
    testImplementation(platform("org.junit:junit-bom:5.7.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    constraints {
        implementation("org.apache.logging.log4j:log4j-core") {
            version {
                strictly("[2.17, 3[")
                prefer("2.17.1")
            }
            because("CVE-2021-44228, CVE-2021-45046, CVE-2021-45105: Log4j vulnerable to remote code execution and other critical security vulnerabilities")
        }
    }
}

pluginBundle {
    website = "https://github.com/chklauser/sjsonnet-gradle-plugin"
    vcsUrl = "https://github.com/chklauser/sjsonnet-gradle-plugin.git"
    tags = listOf("jsonnet", "sjsonnet", "json", "generator")
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

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/chklauser/sjsonnet-gradle-plugin")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
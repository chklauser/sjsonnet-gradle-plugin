rootProject.name = "sjsonnet-gradle-plugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "ajoberstar-backup"
            url = uri("https://ajoberstar.org/bintray-backup/")
        }
    }
}

plugins {
    id("org.ajoberstar.reckon.settings") version "0.18.0"
}

extensions.configure<org.ajoberstar.reckon.gradle.ReckonExtension> {
    setDefaultInferredScope("patch")
    stages("beta", "rc", "final")
    setScopeCalc(calcScopeFromProp().or(calcScopeFromCommitMessages()))
    setStageCalc(calcStageFromProp())
}

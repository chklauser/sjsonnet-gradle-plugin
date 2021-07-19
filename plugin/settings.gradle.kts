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
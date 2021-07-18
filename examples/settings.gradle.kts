// Include the plugin build (composite build)
rootProject.name = "examples"

pluginManagement {
    includeBuild("../plugin")
}

include("default-task")

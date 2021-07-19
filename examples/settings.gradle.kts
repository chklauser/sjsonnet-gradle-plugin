// Include the plugin build (composite build)
rootProject.name = "examples"

pluginManagement {
    includeBuild("../plugin")
}

include("default-task")

val isCiServer = System.getenv().containsKey("CI")
// Cache build artifacts, so expensive operations do not need to be re-computed
buildCache {
    local {
        isEnabled = !isCiServer
    }
}

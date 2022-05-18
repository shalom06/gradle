pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
setup()
fun setup() {
    rootProject.name = "Gradle Plugin"
    include(":app")
    include(":library_one")
    include(":library_two")

    /** creates a list of all the modules present in the project */
    val libs = mutableListOf<Module>()
    rootProject.children.forEach { lib ->
        libs.add(Module(lib.name.toString(), lib.path.toString()))
    }
    /** We use the projectsLoaded gradle hook to load the list of modules into memory
    by creating an extension called modules everytime the project is loaded
    this information can be used in gradle plugins and scripts **/
    gradle.projectsLoaded {
        rootProject.extensions.add(
            "modules",
            libs.map {
                mapOf(
                    "name" to it.name,
                    "path" to it.path
                )
            }
        )
    }
}

private data class Module(
    val name: String,
    val path: String
)
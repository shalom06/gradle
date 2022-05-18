import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.security.MessageDigest

@Suppress("UNCHECKED_CAST")
class GenerateConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("generateModuleTestsConfig") {

            val raw = project.extensions.getByName("modules") as List<Map<String, Any>>
            val modules = raw.map { data ->
                Module(
                    name = data["name"] as String,
                    path = data["path"] as String,
                )
            }.filter { it.name.startsWith("library") }

            val folder = File(project.projectDir, ".circleci")
            val template = File(folder, "config_template.yml")
            val data = buildString {
                append(template.readText())
                modules.forEach {
                    append(project.generateModuleBuildConfiguration(it))
                }
            }

            project.writeBuildConfiguration( data)

        }
    }

}

private data class Module(
    val name: String,
    val path: String
) {
    /** Gets the relative file path **/
    fun getFilePath() = path.substring(1).replace(":", "/")

    /** we need to do this because downloading dependencies everytime
     * is a very un reliable process and can time out unexpectedly
     * we use this key to to determine if the build gradle has been updated
     **/
    fun createDependencyCacheKey(
        rootDir: File
    ): String {
        val modulePath = File(rootDir, getFilePath())

        val rootBuildFile = File(rootDir, "build.gradle")
        val moduleBuildFile = File(modulePath, "build.gradle.kts")

        return listOf(
            "deps",
            sha1(rootBuildFile),
            sha1(moduleBuildFile)
        ).joinToString(separator = "-")
    }
}

private fun Project.generateModuleBuildConfiguration(
    library: Module
): String {
    val tasks = listOf(
        "assembleRelease",
        "assembleAndroidTest",
        "lintRelease",
        "testRelease"
    )

    val gradleTasks = tasks.joinToString(separator = " ") { task ->
        "${library.path}:$task"
    }

    return """
      - build_test_check:
          name: ${library.name}
          moduleName: "${library.path}"
          dependencyCacheKey: ${library.createDependencyCacheKey(project.rootDir)}
          gradleTasks: "$gradleTasks"
    
          filters:
            tags:
              only: /^v[0-9]+(\.[0-9]+)*(-.*)?${'$'}/"""
}

private fun sha1(file: File): String {
    return MessageDigest.getInstance("SHA-1").digest(file.readBytes())
        .joinToString("") { "%02x".format(it) }
}

private fun Project.writeBuildConfiguration(configuration: String) {
    val folder = File(project.projectDir, ".circleci")
    val file = File(folder, "generated_config.yml")
    file.writeText(configuration)
}

import org.gradle.api.Project
import java.io.File

object Utils {

    class DotenvConfiguration(private val rootDir: File, private val fileName: String = DEFAULT_ENV_FILE_NAME) {

        fun environmentVariables(): Map<String, String> =
            rootDir.resolve(fileName)
                .takeIf { it.exists() }
                ?.readLines()
                ?.filter { it.isNotBlank() && !it.startsWith(COMMENT_SYMBOL) }
                ?.associate { it.split(KEY_VALUE_SEPARATOR).let { (key, value) -> key to value } }
                ?: emptyMap()

        companion object {
            private const val DEFAULT_ENV_FILE_NAME = ".env"
            private const val COMMENT_SYMBOL = "#"
            private const val KEY_VALUE_SEPARATOR = "="
        }
    }

    fun Project.envs() = DotenvConfiguration(projectDir).environmentVariables()

}
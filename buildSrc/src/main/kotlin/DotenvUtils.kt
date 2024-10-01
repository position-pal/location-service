import org.gradle.api.Project
import org.gradle.process.ProcessForkOptions
import java.io.File

object DotenvUtils {

    fun Project.dotenv(): DotenvConfiguration = DotenvConfiguration(projectDir)

    class DotenvConfiguration(private val rootDir: File, private val fileName: String = DEFAULT_ENV_FILE_NAME) {

        fun environmentVariables(): Map<String, String> =
            rootDir.resolve(fileName)
                .takeIf { it.exists() }
                ?.readLines()
                ?.filter { it.isNotBlank() && !it.startsWith(COMMENT_SYMBOL) }
                ?.associate { it.split(KEY_VALUE_SEPARATOR).let { (key, value) -> key to value } }
                ?: emptyMap()

        fun applyTo(vararg pfo: ProcessForkOptions) =
            environmentVariables().forEach { (key, value) -> pfo.forEach { it.environment(key, value) } }

        companion object {
            private const val DEFAULT_ENV_FILE_NAME = ".env"
            private const val COMMENT_SYMBOL = "#"
            private const val KEY_VALUE_SEPARATOR = "="
        }
    }
}
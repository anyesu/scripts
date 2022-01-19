@file:Suppress("PropertyName")

/*
 * Provides a Task to move local Gradle caches to local Maven caches, so that the caches can be shared with Maven, and the directory structure of Maven is easier to search.
 *
 * You need to set the environment variable `M2_HOME` first, otherwise it will be moved to the default directory `{user.home}.m2` .
 *
 * gradlew cacheToLocalMavenRepository
 *
 * @see https://blog.csdn.net/feinifi/article/details/81458639
 *
 * @author anyesu <https://github.com/anyesu>
 */

val GROUP_NAME = "init.gradle"
val TASK_NAME = "cacheToLocalMavenRepository"
val TASK_DESC = "Move local Gradle caches to local Maven caches"

projectsLoaded {
    rootProject {
        tasks.register<Copy>(TASK_NAME) {
            group = GROUP_NAME
            description = TASK_DESC

            val gradleCache = File(gradleUserHomeDir, "caches/modules-2/files-2.1")
            from(gradleCache)
            into(repositories.mavenLocal().url)
            eachFile {
                val parts = ArrayList(path.split("/"))
                parts[0] = parts[0].replace('.', '/') // split path
                if (parts.size > 3) parts.removeAt(3) // remove random values
                path = parts.joinToString("/")
            }
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            includeEmptyDirs = false

            doLast {
                gradleCache.deleteRecursively()
            }
        }
        // auto execute when run `gradlew`
        // defaultTasks(TASK_NAME)
    }
}

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
        val groupIdExcludes = setOf("gradle", "org.codehaus.groovy", "com.squareup.okio")
        val artifactIdExcludes = setOf("kotlin-stdlib", "unzipped.com.jetbrains.plugins")

        tasks.register<Copy>(TASK_NAME) {
            group = GROUP_NAME
            description = TASK_DESC

            val gradleCache = File(gradleUserHomeDir, "caches/modules-2/files-2.1")
            from(gradleCache)
            into(repositories.mavenLocal().url)
            eachFile {
                val parts = ArrayList(path.split("/"))
                if (parts.size < 2 || groupIdExcludes.contains(parts[0]) || artifactIdExcludes.contains(parts[1])) {
                    exclude()
                    return@eachFile
                }

                parts[0] = parts[0].replace('.', '/') // split path
                if (parts.size > 3) parts.removeAt(3) // remove random values
                path = parts.joinToString("/")
            }
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            includeEmptyDirs = false

            doLast {
                gradleCache.listFiles()?.flatMap {
                    if (groupIdExcludes.contains(it.name) || !it.isDirectory) {
                        return@flatMap emptyList<File>()
                    }
                    it.listFiles { f ->
                        !artifactIdExcludes.contains(f.name)
                    }?.toList() ?: emptyList()
                }?.forEach { it.deleteRecursively() }

                // delete empty dirs
                gradleCache.listFiles { f ->
                    f.isDirectory && f.list()?.isEmpty() ?: false
                }?.forEach { it.delete() }
            }

            // always run task (no cache), ref: https://github.com/gradle/gradle/issues/9095
            outputs.upToDateWhen { false }
        }
        // auto execute when run `gradlew`
        // defaultTasks(TASK_NAME)
    }
}

/*
 * Provides a listener so that `buildSrc` could run same task from rootProject.
 *
 * set property before apply this script: System.setProperty("init.gradle.buildSrc.tasks", "showRepos,showPlugins")
 *
 * @author anyesu <https://github.com/anyesu>
 */

class Configs(@Suppress("MemberVisibilityCanBePrivate") val dir: String = "") : HashMap<String, Boolean>() {
    var formatter: Configs.(String) -> String = { "$dir.$it.tmp" }

    private fun configFile(key: String) = File(formatter(key))

    override operator fun get(key: String) = super.get(key) ?: configFile(key).exists()

    override fun put(key: String, value: Boolean) = configFile(key).run {
        if (value) {
            parentFile.mkdirs()
            if (!exists()) createNewFile()
        } else if (exists()) {
            delete()
        }
        super.put(key, value)
    }

    operator fun set(key: String, value: Boolean) = put(key, value)

    fun bind(key: String) = ConfigsDelegate(this, key)

    class ConfigsDelegate(private val configs: Configs, private val key: String) :
        kotlin.properties.ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = configs[key]

        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Boolean) {
            configs[key] = value
        }
    }
}

val projectConfigs by lazy {
    val projectDir = rootProject.run { if (isBuildSrc) rootDir.parentFile else rootDir }
    Configs("${projectDir.path}/build/.buildSrc.init.gradle")
}

val Project.isBuildSrc get() = name == "buildSrc"

class BuildSrcFixer(private val taskNames: Iterable<String>) : BuildAdapter() {
    override fun projectsLoaded(gradle: Gradle) {
        // Indicates that whether the task is running
        startParameter.taskNames.intersect(taskNames.toSet()).forEach { isTaskRunning(it, true) }

        rootProject {
            if (!isBuildSrc) return@rootProject

            taskNames.forEach {
                if (isTaskRunning(it)) {
                    try {
                        tasks.named("build") { dependsOn(it) }
                    } catch (e: Throwable) {
                        println(e.message)
                    }
                }
            }
        }
    }

    override fun buildFinished(result: BuildResult) {
        startParameter.taskNames.forEach { isTaskRunning(it, false) }
    }
}

fun isTaskRunning(name: String) = projectConfigs[name]

fun isTaskRunning(name: String, running: Boolean) {
    projectConfigs[name] = running
}

val tasks = System.getProperty("init.gradle.buildSrc.tasks").orEmpty().split(",")
addListener(BuildSrcFixer(tasks))

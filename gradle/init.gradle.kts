/*
 * Auto configure Gradle's proxy from environment variables
 *
 * test: gradlew showIP
 *
 * @author anyesu <https://github.com/anyesu>
 */

import org.gradle.util.GradleVersion

initscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

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

val configs = Configs("${gradleUserHomeDir.path}/init.d/")

// enforce use of TLSv1.2
// may cause 'The server may not support the client's requested TLS protocol versions: (TLSv1.2, TLSv1.3). You may need to configure the client to allow other protocols to be used. See: https://docs.gradle.org/current/userguide/build_environment.html#gradle_system_properties'
System.setProperty("https.protocols", "TLSv1.2")

val GROUP_NAME = "init.gradle scripts"
var BASE_PATH = "E:\\github\\anyesu\\scripts\\gradle\\init.d\\"
// BASE_PATH = "https://cdn.jsdelivr.net/gh/anyesu/scripts/gradle/init.d/"
// BASE_PATH = "https://raw.fastgit.org/anyesu/scripts/main/gradle/init.d/"
// BASE_PATH = "https://cdn.staticaly.com/gh/anyesu/scripts@main/gradle/init.d/"

val scriptPaths = listOf<String>(
    "0.repos.init.gradle.kts",
    "buildSrc.init.gradle.kts",
    "cacheToLocalMavenRepository.init.gradle.kts",
    "dependency-graph.init.gradle.kts",
    "dependencyUpdates.init.gradle.kts",
    "plugins.init.gradle.kts",
    "proxy.init.gradle.kts"
).mapTo(ArrayList()) { "$BASE_PATH$it" }

class Script(val key: String, val path: String) {
    var disabled by configs.bind(key)
    val desc get() = "Enable/Disable script - [ current: ${if (disabled) "disabled" else "enabled"} ]\n$path"

    fun apply() = apply {
        try {
            if (!disabled) gradle.apply(from = path)
            createTask()
        } catch (e: Throwable) {
            "Could not apply the script '$path'".also {
                val logLevel = gradle.startParameter.logLevel
                if (listOf(LogLevel.INFO, LogLevel.DEBUG).contains(logLevel)) {
                    logger.error(it, e)
                } else {
                    println(it)
                    var ex: Throwable? = e
                    var i = 0
                    while (ex != null) {
                        println("${">".padStart(1 + i++ * 3)} ${ex.message}")
                        ex = ex.cause
                    }
                    println("Run with --info or --debug option to get more log output.")
                }
            }
        }
    }

    fun createTask() = projectsLoaded {
        rootProject.tasks.register("init.gradle-$key") {
            group = GROUP_NAME
            description = desc
            doLast {
                disabled = !disabled
                description = desc
            }
        }
    }
}

val scripts = linkedMapOf<String, Script>().apply {
    scriptPaths.map {
        val name = File(it).name
        var key = name
        var index = 1
        while (contains(key)) {
            key = name + ++index
        }
        put(key, Script(key, it))
    }
}

fun check(version: String) = try {
    GradleVersion.current() >= GradleVersion.version(version)
} catch (e: Throwable) {
    false
}

fun main() {
    "7.0".also {
        if (!check(it)) {
            println(gradle.startParameter.allInitScripts)
            println("Skip Initialization Script: requires Gradle \"$it\" or newer, current is \"$gradleVersion\" .")
            return
        }
    }

    val rootScript = "init.gradle.kts"
    var disabled by configs.bind(rootScript)
    if (!disabled) scripts.values.forEach { it.apply() }
    projectsLoaded {
        rootProject.tasks.register(rootScript) {
            group = GROUP_NAME
            description = "Enable/Disable $rootScript - [ current: ${if (disabled) "disabled" else "enabled"} ]"
            doLast {
                disabled = !disabled
            }
        }
    }
}

main()

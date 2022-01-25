@file:Suppress("PropertyName", "SpellCheckingInspection")

/*
* 国内的网络环境默认不设置镜像加速的话很大概率无法下载依赖包导致初始化报错，对于刚入门的新手来说直接劝退。
*
* 本脚本全局配置所有项目使用镜像源（阿里云、腾讯云、华为云）进行加速，这样就不用每个项目重复设置了，新项目拉下来就能跑。
*
* 文件名以 0 开头是为了把这个脚本的执行顺序排在前面。
*
* gradlew showRepos
* gradlew toggleDeleteRawRepos
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

val configs = Configs("${gradleUserHomeDir.path}/init.d/.repos.init.gradle")

/** Whether to delete the source repositories configured in the project */
var deleteRawRepos by configs

val GROUP_NAME = "init.gradle"
val TASK_NAME_SHOW_REPOS = "showRepos"
val TASK_DESC_SHOW_REPOS = "Displays the repositories of project"
val TASK_NAME_TOGGLE_DELETE_RAW_REPOS = "toggleDeleteRawRepos"
val TASK_DESC_TOGGLE_DELETE_RAW_REPOS =
    "Enable/Disable - Whether to delete the source repositories configured in the project"

fun fixBuildSrcTask(name: String) {
    val key = "init.gradle.buildSrc.tasks"
    val tasks = System.getProperty(key).orEmpty().split(",")
        .filter { it.isNotBlank() }.toMutableList().apply { add(name) }
    System.setProperty(key, tasks.joinToString())
}
fixBuildSrcTask(TASK_NAME_SHOW_REPOS)

// collection of all repositories
val collection = LinkedHashMap<String, ArrayList<MavenArtifactRepository>>()

// source repositories
val rawRepos = setOf(
    // central
    "https://repo1.maven.org/maven2/",
    "https://repo.maven.apache.org/maven2/",
    // jcenter
    "https://jcenter.bintray.com/",
    // google
    "https://maven.google.com/",
    "https://dl.google.com/dl/android/maven2/",
    // gradle-plugin
    "https://plugins.gradle.org/m2/"
)

// mirror repositories
object Repos {
    const val MAVEN_ALIYUN = "https://maven.aliyun.com/repository/public"
    const val MAVEN_TENCENT = "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/"
    const val MAVEN_HUAWEI = "https://repo.huaweicloud.com/repository/maven/"
    const val MAVEN_JITPACK = "https://www.jitpack.io"
    const val GOOGLE_ALIYUN = "https://maven.aliyun.com/repository/google"
    const val GRADLE_PLUGIN_ALIYUN = "https://maven.aliyun.com/repository/gradle-plugin"
    const val GRADLE_PLUGIN_ALIYUN_OLD = "https://maven.aliyun.com/nexus/content/repositories/gradle-plugin"
}

projectsLoaded {
    allprojects { configureRepos() }
    rootProject {
        tasks.register(TASK_NAME_SHOW_REPOS) {
            group = GROUP_NAME
            description = TASK_DESC_SHOW_REPOS
            doFirst { displayScriptConfigs() }
            doLast {
                val maxLength = collection.values.flatMap { it.map { r -> r.name.length } }.maxOrNull() ?: 0
                collection.forEach { (k, v) ->
                    println("\n[ $k ]")
                    v.forEach { println("  - ${it.display(maxLength)}") }
                }
            }
        }
        tasks.register(TASK_NAME_TOGGLE_DELETE_RAW_REPOS) {
            group = GROUP_NAME
            description = TASK_DESC_TOGGLE_DELETE_RAW_REPOS
            doLast {
                deleteRawRepos = !deleteRawRepos
                displayScriptConfigs()
            }
        }
    }
}

beforeSettings { configurePluginManagement() }

fun displayScriptConfigs() {
    println("[ Current Initialization Script Configs ]")
    println("  - deleteRawRepos [${if (deleteRawRepos) "enabled" else "disabled"}]")
}

fun Settings.configurePluginManagement() = pluginManagement.repositories {
    collectRepositories(group = "pluginManagement.repositories")
    gradlePluginPortal()
    // TODO aliyun may cause 'Could not find xxx Searched in the following locations...'
    maven(Repos.GRADLE_PLUGIN_ALIYUN)
    maven(Repos.GRADLE_PLUGIN_ALIYUN_OLD)
}

fun Project.configureRepos() {
    buildscript.repositories.configureRepos("$name - buildscript.repositories")
    repositories.configureRepos("$name - repositories")
}

fun RepositoryHandler.configureRepos(group: String = "") {
    collectRepositories(group)
    mavenLocal()
    mavenCentral { url = uri(Repos.MAVEN_ALIYUN) }
    maven(Repos.MAVEN_ALIYUN)
    maven(Repos.MAVEN_TENCENT)
    maven(Repos.MAVEN_HUAWEI)
    google { url = uri(Repos.GOOGLE_ALIYUN) }
    maven(Repos.MAVEN_JITPACK)
    @Suppress("DEPRECATION") jcenter { url = uri(Repos.MAVEN_ALIYUN) }
    mavenCentral()
    google()
    // JCenter shutdown, ref: https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter
    // jcenter()
}

fun RepositoryHandler.collectRepositories(group: String = "") = all {
    if (this !is MavenArtifactRepository) return@all

    val path = url.toString().run { if (endsWith("/")) this else "$this/" }
    if (deleteRawRepos && rawRepos.contains(path)) {
        logger.info("removed from `${group}`: ${display()}")
        remove(this)
    } else {
        val repositories = collection.getOrPut(group) { ArrayList() }
        if (repositories.map { it.url }.contains(url)) {
            logger.info("removed from `${group}`: ${display()}")
            remove(this)
        } else {
            repositories.add(this)
        }
    }
}

fun MavenArtifactRepository.display(size: Int = 0) = "[ ${name.padEnd(size)} ] -> $url"

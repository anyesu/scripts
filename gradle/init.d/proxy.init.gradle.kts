@file:Suppress("PropertyName", "SpellCheckingInspection")

/*
 * Auto configure Gradle's proxy from environment variables.
 *
 * test: gradlew showIP
 *
 * @author anyesu <https://github.com/anyesu>
 */

import de.undercouch.gradle.tasks.download.Download
import nl.martijndwars.proxy.ProxyGradlePluginPlugin

val GROUP_NAME = "init.gradle"
val TASK_NAME = "showIP"
val TASK_DESC = "Show IP Information"

initscript {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("nl.martijndwars:proxy-gradle-plugin:1.0.1")
        classpath("de.undercouch:gradle-download-task:4.1.2")
    }
}

projectsLoaded {
    rootProject {
        tasks.register<Download>(TASK_NAME) {
            group = GROUP_NAME
            description = TASK_DESC
            src("https://myip.ipip.net")
            dest(File(buildDir, "ip.txt"))
            doLast {
                outputFiles.forEach {
                    println(it.readText())
                    it.delete()
                }
            }
        }
    }
}

// applied to daemon, should be reset every time
apply<ProxyGradlePluginPlugin>()

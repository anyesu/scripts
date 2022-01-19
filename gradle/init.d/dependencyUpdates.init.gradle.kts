@file:Suppress("PropertyName")

/*
 * Provides a task to determine which dependencies have updates.
 *
 * gradlew dependencyUpdates
 *
 * @see https://github.com/ben-manes/gradle-versions-plugin#using-a-gradle-init-script
 *
 * @author anyesu <https://github.com/anyesu>
 */

import com.github.benmanes.gradle.versions.VersionsPlugin
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

val GROUP_NAME = "init.gradle"
val PLUGIN_ID = "com.github.ben-manes.versions"

initscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }

    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:+")
    }
}

projectsEvaluated {
    allprojects {
        if (!plugins.hasPlugin(PLUGIN_ID)) {
            configureDependencyUpdates()
            logger.info("project '$name' apply plugin: $PLUGIN_ID")
        }
    }
}

val String.isNonStable
    get() = listOf("final", "rc", "m", "alpha", "beta", "ga").any {
        val regex = "^(?i).*[.-]${it}[.\\d-]*$".toRegex()
        regex.matches(this)
    }

// The version number starts with `v`, e.g.: `v1.2.3`
val String.startsWithV get() = startsWith("v")

// do not check dependencies of AGP
val ModuleComponentIdentifier.isAgp get() = group.startsWith("com.android.tools")

fun Project.configureDependencyUpdates() {
    apply<VersionsPlugin>()
    tasks.withType<DependencyUpdatesTask> {
        group = GROUP_NAME
        rejectVersionIf {
            candidate.isAgp ||
                !currentVersion.isNonStable && candidate.version.isNonStable ||
                currentVersion.startsWithV && !candidate.version.startsWithV
        }
    }
}

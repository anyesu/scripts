@file:Suppress("PropertyName")

/*
 * Lets you visualize your dependencies in a graph.
 *
 * gradlew generateDependencyGraph
 * gradlew generateProjectDependencyGraph
 *
 * @author anyesu <https://github.com/anyesu>
 */

val PLUGIN_ID = "com.vanniktech.dependency.graph.generator"

projectsLoaded {
    allprojects {
        buildscript {
            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                classpath("com.vanniktech:gradle-dependency-graph-generator-plugin:+")
            }
        }

        afterEvaluate {
            if (!plugins.hasPlugin(PLUGIN_ID)) {
                apply(plugin = PLUGIN_ID)
                logger.info("project '$name' apply plugin: $PLUGIN_ID")
            }
        }
    }
}

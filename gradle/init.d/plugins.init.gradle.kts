@file:Suppress("PropertyName")

import java.awt.Color

/*
 * Displays all plugins.
 *
 * gradlew showPlugins
 *
 * @author anyesu <https://github.com/anyesu>
 */

val GROUP_NAME = "init.gradle"
val TASK_NAME = "showPlugins"
val TASK_DESC = "Displays the plugins of project"

// simple implementation of ansi, ref: https://github.com/chalk/chalk/blob/main/source/vendor/ansi-styles/index.js
val FLAG = "\u001B["
val FLAG_RESET = "${FLAG}0m"
fun Color.ansi16m(str: String, offset: Int = 0) =
    run { "$FLAG${38 + offset};2;$red;$green;${blue}m$str$FLAG_RESET" }

fun Color.ansi16mBg(str: String) = ansi16m(str, 10)

fun Int.ansi16(str: String, offset: Int = 0) = let { "$FLAG${it + offset}m$str$FLAG_RESET" }
fun Int.ansi16Bg(str: String) = ansi16(str, 10)
fun Int.ansi256(str: String, offset: Int = 0) = let { "$FLAG${38 + offset};5;${it}m$str$FLAG_RESET" }
fun Int.ansi256Bg(str: String) = ansi256(str, 10)

/**
 * showAllColors(10, 11) { code, str -> code.ansi16(str) }
 * showAllColors(16, 16) { code, str -> code.ansi256(str) }
 */
fun showAllColors(w: Int, h: Int, ansi: (Int, String) -> String) {
    (0 until h).forEach { i ->
        (0 until w).forEach { j ->
            val code = i * w + j
            print(ansi(code, String.format("%-4d", code)))
        }
        println("")
    }
}

projectsLoaded {
    allprojects {
        tasks.register(TASK_NAME) {
            group = GROUP_NAME
            description = TASK_DESC
            doLast {
                plugins.sortedBy { it.toString() }.forEach {
                    val name = it.toString().split("@").firstOrNull().orEmpty()
                    println(33.ansi16(name))
                }
            }
        }
    }
}

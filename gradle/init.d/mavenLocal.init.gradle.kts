/*
* Prepend `mavenLocal()` to each `repositories`
*
* @author anyesu <https://github.com/anyesu>
*/

allprojects {
    buildscript {
        repositories {
            mavenLocal()
        }
    }
    repositories {
        mavenLocal()
    }
}

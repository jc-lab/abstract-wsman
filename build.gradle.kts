plugins {
    kotlin("jvm") version "1.8.21"
}

val projectGroup = "kr.jclab.wsman"
val projectVersion = Version.PROJECT

group = projectGroup
version = projectVersion

allprojects {
    group = projectGroup
    version = projectVersion
}

repositories {
    mavenCentral()
}

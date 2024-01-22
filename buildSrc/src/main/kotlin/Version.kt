import kr.jclab.gradlehelper.ProcessHelper

fun getVersionFromGit(): String {
    return runCatching {
        val version = (
                System.getenv("CI_COMMIT_TAG")
                    ?.takeIf { it.isNotEmpty() }
                    ?: ProcessHelper.executeCommand(listOf("git", "describe", "--tags"))
                        .split("\n")[0]
                )
            .trim()
        if (version.startsWith("v")) {
            version.substring(1)
        } else version
    }.getOrElse {
        return runCatching {
            return ProcessHelper.executeCommand(listOf("git", "rev-parse", "HEAD"))
                .split("\n")[0].trim() + "-SNAPSHOT"
        }.getOrElse {
            return "unknown"
        }
    }
}

object Version {
    val KOTLIN = "1.8.21"
    val BCPROV = "jdk18on:1.73"
    val NETTY = "4.1.99.Final"
    val APACHE_CXF = "4.0.3"
    val PROJECT by lazy { getVersionFromGit() }
}
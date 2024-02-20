plugins {
    `java-library`
    `maven-publish`
    `signing`

    kotlin("jvm") version Version.KOTLIN
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.apache.httpcomponents.client5:httpclient5:5.3")

    api("org.apache.cxf:cxf-rt-frontend-jaxws:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-transports-http:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-transports-local:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-transports-http-netty-client:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-ws-mex:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-ws-addr:${Version.APACHE_CXF}")

    api(project(":abstract-wsman"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set("abstract wsman")
                url.set("https://github.com/jc-lab/abstract-wsman")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("jclab")
                        name.set("Joseph Lee")
                        email.set("joseph@jc-lab.net")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/jc-lab/abstract-wsman.git")
                    developerConnection.set("scm:git:ssh://git@github.com/jc-lab/abstract-wsman.git")
                    url.set("https://github.com/jc-lab/abstract-wsman")
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if ("$version".endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = findProperty("ossrhUsername") as String?
                password = findProperty("ossrhPassword") as String?
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

tasks.withType<Sign>().configureEach {
    onlyIf { project.hasProperty("signing.gnupg.keyName") || project.hasProperty("signing.keyId") }
}
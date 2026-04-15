import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.collections.plus

plugins {
    `java-library`
    `maven-publish`
    `signing`

    kotlin("jvm") version Version.KOTLIN
    id("io.mateo.cxf-codegen") version "2.5.0"
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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "1.8"
    }
}

dependencies {
    cxfCodegen(platform("org.apache.cxf:cxf-bom:4.0.3"))
    cxfCodegen("jakarta.xml.ws:jakarta.xml.ws-api:4.0.1")
    cxfCodegen("jakarta.annotation:jakarta.annotation-api:2.1.1")
    cxfCodegen("org.apache.cxf:cxf-xjc-plugin:4.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.apache.httpcomponents.client5:httpclient5:5.3")

    api("org.apache.cxf:cxf-rt-frontend-jaxws:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-transports-http:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-transports-local:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-ws-mex:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-ws-addr:${Version.APACHE_CXF}")
}

cxfCodegen {
    cxfVersion.set("4.0.3")
}

val wsdlListFile = file("$buildDir/wsdlList.tmp")
val wsdlCatalogFile = file("$buildDir/wsdl2java-catalog.txt")
val eventingCodegenWsdlFile = file("$buildDir/eventing-codegen.wsdl")
tasks.register("generateWsdl", io.mateo.cxf.codegen.wsdl2java.Wsdl2Java::class) {
    doFirst {
        val eventingSchemaUri = file("$projectDir/src/main/resources/xsd/eventing.xsd").toURI().toString()
        val eventingCodegenWsdl = file("$projectDir/src/main/resources/xsd/eventing.wsdl")
            .readText()
            .replace("schemaLocation='eventing.xsd'", "schemaLocation='$eventingSchemaUri'")
            .replace(
                Regex(
                    """\s*<wsdl:operation name='SubscriptionEnd' >.*?</wsdl:operation>\s*""",
                    setOf(RegexOption.DOT_MATCHES_ALL)
                ),
                "\n"
            )
        eventingCodegenWsdlFile.writeText(eventingCodegenWsdl)

        file("$projectDir/src/main/resources/xsd/eventing.xsd")
            .copyTo(eventingCodegenWsdlFile.parentFile.resolve( "eventing.xsd"), overwrite = true)

        wsdlListFile.writeText(
            listOf(
                file("$projectDir/src/main/resources/xsd/enumeration.wsdl"),
                file("$projectDir/src/main/resources/xsd/wsman.wsdl"),
                file("$projectDir/src/main/resources/xsd/transfer.wsdl"),
                eventingCodegenWsdlFile
            ).joinToString("\n") { it.toURI().toString() }
        )
        wsdlCatalogFile.writeText(
            """
            SYSTEM "http://schemas.xmlsoap.org/ws/2004/08/addressing" "${file("$projectDir/src/main/resources/xsd/addressing.xsd").toURI()}"
            SYSTEM "http://schemas.xmlsoap.org/ws/2004/08/eventing" "${file("$projectDir/src/main/resources/xsd/eventing.xsd").toURI()}"
            """.trimIndent()
        )
    }
//    outputs.file(wsdlListFile)
//    outputs.file(wsdlCatalogFile)
//    outputs.file(eventingCodegenWsdlFile)
    toolOptions {
        wsdl.set(wsdlListFile.absolutePath)
        wsdlList.set(true)
        bindingFiles.set(listOf("$projectDir/src/main/resources/global.xjb"))
        catalog.set(wsdlCatalogFile.absolutePath)
        validateWsdl.set("none")
        markGenerated.set(true)
        verbose.set(true)
        extendedSoapHeaders.set(true)
    }
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
            val releasesRepoUrl = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://ossrh-staging-api.central.sonatype.com/content/repositories/snapshots/"
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

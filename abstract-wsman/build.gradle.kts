plugins {
    id("java-library")
    kotlin("jvm") version Version.KOTLIN
    id("io.mateo.cxf-codegen") version "2.2.0" // "1.2.1"
}

repositories {
    mavenCentral()
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
    api("org.apache.cxf:cxf-rt-transports-http-netty-client:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-ws-mex:${Version.APACHE_CXF}")
    api("org.apache.cxf:cxf-rt-ws-addr:${Version.APACHE_CXF}")

    // https://mvnrepository.com/artifact/jakarta.servlet/jakarta.servlet-api
    api("jakarta.servlet:jakarta.servlet-api:6.0.0")

    implementation("org.opennms.core.wsman:org.opennms.core.wsman.cxf:1.2.3")

    // SAAJ Message Factory
    implementation("com.sun.xml.messaging.saaj:saaj-impl:3.0.3")
}

cxfCodegen {
    cxfVersion.set("4.0.3")
}

val wsdlListFile = file("$buildDir/wsdlList.tmp")
tasks.register("generateWsdl", io.mateo.cxf.codegen.wsdl2java.Wsdl2Java::class) {
    doFirst {
        wsdlListFile.writeText(
            listOf(
                file("$projectDir/src/main/resources/xsd/enumeration.wsdl"),
                file("$projectDir/src/main/resources/xsd/wsman.wsdl"),
                file("$projectDir/src/main/resources/xsd/transfer.wsdl"),
            )
                .map { it.absolutePath }
                .joinToString("\n")
        )
    }
    outputs.file(wsdlListFile)
    toolOptions {
        wsdl.set(wsdlListFile.absolutePath)
        wsdlList.set(true)
        bindingFiles.set(listOf("$projectDir/src/main/resources/global.xjb"))
        catalog.set("$projectDir/src/main/resources/wsdl2java-catalog.txt")
        markGenerated.set(true)
        verbose.set(true)
        extendedSoapHeaders.set(true)
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
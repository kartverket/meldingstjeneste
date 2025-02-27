val kotlin_version: String by project
val logback_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
    id("io.ktor.plugin") version "3.1.1"
}

group = "meldingstjeneste"
version = "0.0.1"

application {
    mainClass.set("meldingstjeneste.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.smiley4:ktor-swagger-ui:4.1.6")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-webjars-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-request-validation:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
}

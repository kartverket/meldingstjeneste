val ktorVersion = "3.3.3"

plugins {
    val kotlinVersion = "2.2.21"

    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
    id("io.ktor.plugin") version "3.3.3"
}


group = "no.kartverket.meldingstjeneste"
version = "0.0.1"

application {
    mainClass.set("no.kartverket.meldingstjeneste.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

ktor {
    fatJar {
        allowZip64 = true
    }
}

dependencies {
    implementation("io.github.smiley4:ktor-openapi:5.4.0")
    implementation("io.github.smiley4:ktor-swagger-ui:5.4.0")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-webjars-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-request-validation:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.24")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.1")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    // Entra-ID authentication
    implementation("com.microsoft.graph:microsoft-graph:6.60.0")
    implementation("com.azure:azure-identity:1.18.1")

    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.3.0")
}

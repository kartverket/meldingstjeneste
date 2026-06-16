val ktorVersion = "3.5.0"

plugins {
    val kotlinVersion = "2.3.21"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("io.ktor.plugin") version "3.5.0"
}

group = "no.kartverket.meldingstjeneste"
version = "0.0.1"
val javaVersion = 25

application {
    mainClass.set("no.kartverket.meldingstjeneste.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf(
        "-Dio.ktor.development=$isDevelopment",
        "--enable-native-access=ALL-UNNAMED"
    )
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
    implementation("io.github.smiley4:ktor-openapi:5.7.0")
    implementation("io.github.smiley4:ktor-swagger-ui:5.7.0")

    // Server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-webjars:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-request-validation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")

    // Klient
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Serialisering
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.34")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus:1.17.0")

    // Div
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

    // Entra-ID autentisering
    implementation("com.microsoft.graph:microsoft-graph:6.65.0")
    implementation("com.azure:azure-identity:1.18.3")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.21")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

kotlin {
    jvmToolchain(javaVersion)

    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    useJUnitPlatform()
}
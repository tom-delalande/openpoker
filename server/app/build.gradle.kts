import org.gradle.kotlin.dsl.sourceSets

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("ch.acanda.gradle.fabrikt") version "1.31.2"
    application
}

kotlin {
}

sourceSets {
    main {
        kotlin {
            // Register the generated directory as a source directory
            srcDir("$buildDir/generated/src/main/kotlin")
        }
    }
}


application {
    mainClass.set("app.MainKt")
    applicationName = "app"
}

tasks.distZip {
    enabled = false
}

tasks.distTar {
    archiveFileName.set("app-bundle.${archiveExtension.get()}")
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}

val ktorVersion = "3.4.2"
val wireVersion = "6.2.0"
val grpcVersion = "1.80.0"

dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.0-beta1")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

    implementation("redis.clients:jedis:7.4.1")
    implementation("org.postgresql:postgresql:42.7.10")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.0-M1")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-server-core:$ktorVersion")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-websockets:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
}

fabrikt {
    generate("schema") {
        apiFile = file("../../api/tsp-output/schema/openapi.yaml")
        basePackage = "server"

        controller {
            generate = enabled
            authentication = enabled
            target = Ktor
        }

        model {
            generate = enabled
            sealedInterfacesForOneOf = enabled
            serializationLibrary = Kotlinx
            validationLibrary = NoValidation
        }
    }
}
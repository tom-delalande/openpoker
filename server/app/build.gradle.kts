buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.squareup.wiregrpcserver:server-generator:1.0.0-alpha04")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("com.squareup.wire") version "5.3.3"
    application
}

kotlin {
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

val ktorVersion = "2.3.7"
val wireVersion = "6.2.0"
val grpcVersion = "1.80.0"

dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.0-beta1")
    implementation("com.squareup.wire:wire-runtime:$wireVersion")
    implementation("com.squareup.wiregrpcserver:server:1.0.0-alpha04")
    implementation("com.google.protobuf:protobuf-java:4.34.1")
    implementation(platform("io.grpc:grpc-bom:$grpcVersion"))
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-netty-shaded")
    implementation("io.grpc:grpc-kotlin-stub:1.5.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    implementation("redis.clients:jedis:7.4.1")
    implementation("org.postgresql:postgresql:42.7.10")

//    implementation("io.ktor:ktor-server-core:${ktorVersion}")
//    implementation("io.ktor:ktor-server-netty:${ktorVersion}")
//    implementation("io.ktor:ktor-server-content-negotiation:${ktorVersion}")
//    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
//    implementation("io.ktor:ktor-server-websockets:${ktorVersion}")
    testImplementation(kotlin("test"))
}

wire {
    custom {
        schemaHandlerFactory = com.squareup.wire.kotlin.grpcserver.GrpcServerSchemaHandler.Factory()
        options = mapOf(
            "singleMethodServices" to "false",
            "rpcCallStyle" to "suspending",
        )
        exclusive = false
    }
    kotlin {
        rpcRole = "server"
        singleMethodServices = false
        rpcCallStyle = "suspending"
    }
}

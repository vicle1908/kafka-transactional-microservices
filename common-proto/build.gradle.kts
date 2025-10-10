import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.protobuf)
}

dependencies {
    api(libs.grpc.kotlin.stub)
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.junit.jupiter)
}

ktlint {
    filter {
        exclude("**/build/generated/**")
    }
}

tasks.configureEach {
    if (name.startsWith("ktlint")) {
        enabled = false
    }
}

protobuf {
    val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    val protocVersion = libsCatalog.findVersion("protoc").get().requiredVersion
    val protocGenGrpcJava = libsCatalog.findVersion("protoc-gen-grpc-java").get().requiredVersion
    val protocGenGrpcKotlin = libsCatalog.findVersion("protoc-gen-grpc-kotlin").get().requiredVersion
    protoc {
        artifact = "com.google.protobuf:protoc:$protocVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$protocGenGrpcJava"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$protocGenGrpcKotlin:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

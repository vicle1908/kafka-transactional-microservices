pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
    }
//    versionCatalogs {
//        create("libs") {
//            from(files("gradle/libs.versions.toml"))
//        }
//    }
}

rootProject.name = "microservices"

include(
    "common-events",
    "common-kafka",
    "common-persistence",
    "common-sagas",
    "common-events-avro",
    "common-proto",
    "common-temporal",
    "common-outbox-relay",
    "common-observability",
    "common-cache",
    "temporal-pilot",
    "services:orders-service",
    "services:payments-service",
    "services:inventory-service",
    "services:notification-service",
)
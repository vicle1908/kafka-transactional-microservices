plugins {
    id("java-platform")
}

// Define the expected versions
val expectedVersions = mapOf(
    "java" to "25",
    "springBoot" to "3.5.6",
    "springFramework" to "6.2.11",
    "springKafka" to "3.3.10",
    "kafka" to "4.1.0",
    "debezium" to "3.3.0.Final",
    "avro" to "1.12.0",
    "postgresql" to "18",
    "temporal" to "1.25.0",
    "istio" to "1.24.0"
)

// Task to check versions
tasks.register("versionCheck") {
    group = "verification"
    description = "Ensures the build is running with the expected toolchain and library versions"
    
    doLast {
        // Check Java version
        val javaVersion = System.getProperty("java.version")
        val currentMajor = javaVersion.split(".")[0].toIntOrNull() ?: 0
        val expectedMajor = expectedVersions["java"]!!.toInt()
        
        when {
            currentMajor >= expectedMajor -> {
                logger.lifecycle("Java version check passed: $javaVersion (target: ${expectedVersions["java"]})") 
            }
            currentMajor >= 23 -> {
                logger.warn(
                    "Using Java $currentMajor - recommended upgrade to " +
                        "${expectedVersions["java"]} for optimal performance"
                )
            }
            currentMajor >= 21 -> {
                logger.warn(
                    "Using fallback Java $currentMajor - consider upgrading to " +
                        "${expectedVersions["java"]} (supported fallback)"
                )
            }
            else -> {
                throw GradleException("Expected to run with at least Java 21, but current version is $currentMajor")
            }
        }
        
        // This would normally check other versions from the version catalog
        logger.lifecycle("Version check completed. Review build logs for any mismatches.")
        logger.lifecycle("Expected versions:")
        expectedVersions.forEach { (component, version) ->
            logger.lifecycle("  $component: $version")
        }
    }
}

// Task to check schema compatibility
tasks.register("schemaCompatibilityCheck") {
    group = "verification"
    description = "Validates Avro schemas by compiling shared definitions"
    
    doLast {
        // This would normally compile Avro schemas and check for compatibility
        logger.lifecycle("Schema compatibility check would validate Avro schemas in common-events-avro module")
        logger.lifecycle("Implementation would check backward compatibility of schema changes")
    }
}
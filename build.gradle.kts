import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

val libsCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

val detektCli = configurations.maybeCreate("detektCli").apply {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val detektPlugins = configurations.maybeCreate("detektPlugins").apply {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    detektCli(libsCatalog.findLibrary("detekt-cli").get())
    detektPlugins(libsCatalog.findLibrary("detekt-formatting").get())
}

allprojects {
    group = "com.example"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    val libs = rootProject.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    val desiredJavaVersion = (rootProject.findProperty("java.languageVersion") as String?)?.toIntOrNull() ?: 23

    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "jacoco")

    val ktlintVersion = libs.findVersion("ktlint-cli").get().requiredVersion

    extensions.configure(JavaPluginExtension::class.java) {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(desiredJavaVersion))
        }
    }

    configurations.all {
        resolutionStrategy.eachDependency {
            when {
                requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-core" ->
                    useVersion(libs.findVersion("jackson-core").get().requiredVersion)
                requested.group == "org.apache.commons" && requested.name == "commons-compress" ->
                    useVersion(libs.findVersion("commons-compress").get().requiredVersion)
                requested.group == "io.grpc" && requested.name == "grpc-kotlin-stub" ->
                    useVersion(libs.findVersion("grpc-kotlin").get().requiredVersion)
                requested.group == "io.grpc" &&
                    requested.name in setOf(
                        "grpc-stub",
                        "grpc-protobuf",
                        "grpc-netty",
                        "grpc-api",
                        "grpc-services",
                    ) ->
                    useVersion(libs.findVersion("grpc").get().requiredVersion)
                requested.group == "org.junit.jupiter" ->
                    useVersion(libs.findVersion("junit").get().requiredVersion)
                requested.group == "org.junit.platform" ->
                    useVersion(libs.findVersion("junit-platform-launcher").get().requiredVersion)
                requested.group == "org.junit" && requested.name == "junit-bom" ->
                    useVersion(libs.findVersion("junit").get().requiredVersion)
            }
        }
    }

    extensions.configure(KtlintExtension::class.java) {
        version.set(ktlintVersion)
        filter {
            exclude("**/build/**")
            exclude("**/generated/**")
        }
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<JacocoReport>().configureEach {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named("check").configure {
        dependsOn("ktlintCheck", "jacocoTestReport")
        dependsOn(rootProject.tasks.named("detektAll"))
    }
    extensions.configure(DependencyManagementExtension::class.java) {
        imports {
            mavenBom("org.junit:junit-bom:${libs.findVersion("junit").get().requiredVersion}")
            mavenBom("io.opentelemetry:opentelemetry-bom:${libs.findVersion("opentelemetry").get().requiredVersion}")
        }
        dependencies {
            dependency("org.junit.platform:junit-platform-engine:${libs.findVersion("junit-platform-launcher").get().requiredVersion}")
            dependency("org.junit.platform:junit-platform-commons:${libs.findVersion("junit-platform-launcher").get().requiredVersion}")
        }
    }


    dependencies {
        add("testImplementation", platform(libs.findLibrary("junit-bom").get()))
        add("testImplementation", libs.findLibrary("junit-jupiter").get())
        add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
        constraints {
            listOf("implementation", "api").forEach { conf ->
                add(conf, libs.findLibrary("jackson-core").get())
                add(conf, libs.findLibrary("commons-compress").get())
            }
        }
    }
}

tasks.register<Copy>("exportAvroSchemas") {
    description = "Exports shared Avro schema files for registry publication."
    group = "distribution"
    dependsOn(":common-events-avro:generateAvroJava")
    from(rootProject.layout.projectDirectory.dir("common-events-avro/src/main/avro"))
    into(layout.buildDirectory.dir("registry/avro"))
}

tasks.register("versionCheck") {
    group = "verification"
    description = "Ensures the build is running with the expected toolchain"
    doLast {
        val targetJavaVersion = (project.findProperty("java.languageVersion") as String?)?.toIntOrNull() ?: 25
        val fallbackJavaVersion = 21
        val current = JavaVersion.current()
        val target = JavaVersion.toVersion(targetJavaVersion)
        val fallback = JavaVersion.toVersion(fallbackJavaVersion)
        
        when {
            current.isCompatibleWith(target) -> {
                println("✅ Java version check passed: $current (target: $targetJavaVersion)")
            }
            current.isCompatibleWith(fallback) -> {
                println("⚠️  Using fallback Java $current - consider upgrading to $targetJavaVersion (supported fallback)")
            }
            else -> {
                throw GradleException("Expected to run with at least Java $fallbackJavaVersion, but current version is $current")
            }
        }
    }
}

tasks.register("schemaCompatibilityCheck") {
    group = "verification"
    description = "Validates Avro schemas by compiling shared definitions."
    dependsOn(":common-events-avro:build")
}

tasks.register<JavaExec>("detektAll") {
    group = "verification"
    description = "Runs detekt static analysis via CLI across all modules."
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    notCompatibleWithConfigurationCache("Detekt CLI arguments are constructed at execution time.")
    doFirst {
        val detektInputs = listOf(
            "buildSrc",
            "common-events",
            "common-events-avro",
            "common-kafka",
            "common-outbox-relay",
            "common-persistence",
            "common-proto",
            "common-sagas",
            "common-temporal",
            "services",
            "temporal-pilot"
        )
        val inputsArgument = detektInputs.joinToString(",") { projectDir.resolve(it).absolutePath }
        val reportsDir = layout.buildDirectory.dir("reports/detekt").get().asFile
        reportsDir.mkdirs()
        val arguments = mutableListOf(
            "--input", inputsArgument,
            "--config", project.file("config/detekt/detekt.yml").absolutePath,
            "--build-upon-default-config",
            "--parallel",
            "--excludes", "**/build/generated/**",
            "--report", "txt:${reportsDir.resolve("detekt.txt").absolutePath}",
            "--report", "sarif:${reportsDir.resolve("detekt.sarif").absolutePath}"
        )
        val baselineFile = project.file("config/detekt/baseline.xml")
        if (baselineFile.exists()) {
            arguments += listOf("--baseline", baselineFile.absolutePath)
        }
        val pluginClasspath = detektPlugins.resolve()
        if (pluginClasspath.isNotEmpty()) {
            arguments += listOf("--plugins", pluginClasspath.joinToString(",") { it.absolutePath })
        }
        setArgs(arguments)
    }
}

tasks.register<JavaExec>("detektBaseline") {
    group = "verification"
    description = "Generates or refreshes the detekt baseline for existing findings."
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    notCompatibleWithConfigurationCache("Detekt baseline generation resolves arguments at execution time.")
    doFirst {
        val detektInputs = listOf(
            "buildSrc",
            "common-events",
            "common-events-avro",
            "common-kafka",
            "common-outbox-relay",
            "common-persistence",
            "common-proto",
            "common-sagas",
            "common-temporal",
            "services",
            "temporal-pilot"
        )
        val inputsArgument = detektInputs.joinToString(",") { projectDir.resolve(it).absolutePath }
        val baselineFile = project.file("config/detekt/baseline.xml")
        baselineFile.parentFile.mkdirs()
        val pluginClasspath = detektPlugins.resolve()
        val arguments = mutableListOf(
            "--input", inputsArgument,
            "--config", project.file("config/detekt/detekt.yml").absolutePath,
            "--build-upon-default-config",
            "--parallel",
            "--excludes", "**/build/generated/**",
            "--create-baseline",
            "--baseline", baselineFile.absolutePath
        )
        if (pluginClasspath.isNotEmpty()) {
            arguments += listOf("--plugins", pluginClasspath.joinToString(",") { it.absolutePath })
        }
        setArgs(arguments)
    }
}

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
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

abstract class FlywayLintTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val migrations: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Internal
    val repositoryRoot: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun lint() {
        val repoRoot = repositoryRoot.get().asFile
        val migrationFiles = migrations.files.sortedBy { it.invariantSeparatorsPath }

        val duplicates = mutableListOf<String>()

        migrationFiles
            .groupBy { file ->
                val relative = file.relativeTo(repoRoot).invariantSeparatorsPath
                relative.substringBefore("/src/")
            }.forEach { (module, files) ->
                val versions = mutableMapOf<String, MutableList<String>>()
                files.forEach { file ->
                    val match = Regex("^V([0-9]+)__(.+)\\.sql").find(file.name)
                        ?: throw GradleException("Flyway migration has invalid naming: ${file.absolutePath}")
                    val version = match.groupValues[1]
                    versions.computeIfAbsent(version) { mutableListOf() }.add(file.name)
                }
                versions
                    .filter { it.value.size > 1 }
                    .forEach { (version, names) ->
                        duplicates += "Module '$module' defines Flyway version $version multiple times: ${names.joinToString()}"
                    }
            }

        if (duplicates.isNotEmpty()) {
            throw GradleException(duplicates.joinToString(separator = "\n"))
        }

        val forbiddenTokens = listOf("IF NOT EXISTS", "DROP TABLE")
        val offenders =
            migrationFiles.filter { file ->
                val sql = file.readText()
                forbiddenTokens.any { token -> sql.contains(token, ignoreCase = true) }
            }

        if (offenders.isNotEmpty()) {
            val message =
                offenders.joinToString(separator = "\n") {
                    "Forbidden DDL constructs found in ${it.relativeTo(repoRoot).invariantSeparatorsPath}"
                }
            throw GradleException(message)
        }
    }
}

val libsCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

val detektCli =
    configurations.maybeCreate("detektCli").apply {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

val detektPlugins =
    configurations.maybeCreate("detektPlugins").apply {
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
            when (requested.group) {
                "com.fasterxml.jackson.core" if requested.name == "jackson-core" ->
                    useVersion(libs.findVersion("jackson-core").get().requiredVersion)

                "org.apache.commons" if requested.name == "commons-compress" ->
                    useVersion(libs.findVersion("commons-compress").get().requiredVersion)

                "io.grpc" if requested.name == "grpc-kotlin-stub" ->
                    useVersion(libs.findVersion("grpc-kotlin").get().requiredVersion)

                "io.grpc" if requested.name in
                    setOf(
                        "grpc-stub",
                        "grpc-protobuf",
                        "grpc-netty",
                        "grpc-api",
                        "grpc-services",
                    )
                -> useVersion(libs.findVersion("grpc").get().requiredVersion)

                "org.junit.jupiter" ->
                    useVersion(libs.findVersion("junit").get().requiredVersion)

                "org.junit.platform" ->
                    useVersion(libs.findVersion("junit-platform-launcher").get().requiredVersion)

                "org.junit" if requested.name == "junit-bom" ->
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
        dependsOn(rootProject.tasks.named("flywayLint"))
    }
    extensions.configure(DependencyManagementExtension::class.java) {
        imports {
            mavenBom("org.junit:junit-bom:${libs.findVersion("junit").get().requiredVersion}")
            mavenBom("io.opentelemetry:opentelemetry-bom:${libs.findVersion("opentelemetry").get().requiredVersion}")
        }
        dependencies {
            dependency(
                "org.junit.platform:junit-platform-engine:${libs.findVersion(
                    "junit-platform-launcher",
                ).get().requiredVersion}",
            )
            dependency(
                "org.junit.platform:junit-platform-commons:${libs.findVersion(
                    "junit-platform-launcher",
                ).get().requiredVersion}",
            )
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
                println(
                    "⚠️  Using fallback Java $current - consider upgrading to $targetJavaVersion (supported fallback)",
                )
            }
            else -> {
                throw GradleException(
                    "Expected to run with at least Java $fallbackJavaVersion, but current version is $current",
                )
            }
        }
    }
}

val flywayMigrationFiles =
    layout.projectDirectory.asFileTree.matching {
        include("**/src/main/resources/db/migration/V*.sql")
        exclude("**/build/**")
    }

tasks.register<FlywayLintTask>("flywayLint") {
    group = "verification"
    description = "Validates Flyway migrations for duplicate versions and forbidden DDL patterns."
    migrations.from(flywayMigrationFiles)
    repositoryRoot.set(layout.projectDirectory)
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
        val detektInputs =
            listOf(
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
                "temporal-pilot",
            )
        val inputsArgument = detektInputs.joinToString(",") { projectDir.resolve(it).absolutePath }
        val reportsDir =
            layout.buildDirectory
                .dir("reports/detekt")
                .get()
                .asFile
        reportsDir.mkdirs()
        val arguments =
            mutableListOf(
                "--input",
                inputsArgument,
                "--config",
                project.file("config/detekt/detekt.yml").absolutePath,
                "--build-upon-default-config",
                "--parallel",
                "--excludes",
                "**/build/**,**/build/generated/**," +
                    "**/build/generated-sources/**,**/buildSrc/build/generated-sources/**",
                "--report",
                "txt:${reportsDir.resolve("detekt.txt").absolutePath}",
                "--report",
                "sarif:${reportsDir.resolve("detekt.sarif").absolutePath}",
            )
        val baselineFile = project.file("config/detekt/baseline.xml")
        if (baselineFile.exists()) {
            arguments += listOf("--baseline", baselineFile.absolutePath)
        }
        val pluginClasspath = detektPlugins.resolve()
        if (pluginClasspath.isNotEmpty()) {
            arguments += listOf("--plugins", pluginClasspath.joinToString(",") { it.absolutePath })
        }
        args = arguments
    }
}

tasks.register<JavaExec>("detektBaseline") {
    group = "verification"
    description = "Generates or refreshes the detekt baseline for existing findings."
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    notCompatibleWithConfigurationCache("Detekt baseline generation resolves arguments at execution time.")
    doFirst {
        val detektInputs =
            listOf(
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
                "temporal-pilot",
            )
        val inputsArgument = detektInputs.joinToString(",") { projectDir.resolve(it).absolutePath }
        val baselineFile = project.file("config/detekt/baseline.xml")
        baselineFile.parentFile.mkdirs()
        val pluginClasspath = detektPlugins.resolve()
        val arguments =
            mutableListOf(
                "--input",
                inputsArgument,
                "--config",
                project.file("config/detekt/detekt.yml").absolutePath,
                "--build-upon-default-config",
                "--parallel",
                "--excludes",
                "**/build/**,**/build/generated/**," +
                    "**/build/generated-sources/**,**/buildSrc/build/generated-sources/**",
                "--create-baseline",
                "--baseline",
                baselineFile.absolutePath,
            )
        if (pluginClasspath.isNotEmpty()) {
            arguments += listOf("--plugins", pluginClasspath.joinToString(",") { it.absolutePath })
        }
        args = arguments
    }
}

// Fast pre-commit Detekt over only staged Kotlin files
tasks.register<JavaExec>("detektChanged") {
    group = "verification"
    description = "Runs detekt on staged Kotlin files only (fast pre-commit gate)."
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    notCompatibleWithConfigurationCache("Detekt CLI arguments are constructed at execution time.")
    doFirst {
        fun stagedKotlinFiles(): List<String> {
            val pb = ProcessBuilder(listOf("git", "diff", "--cached", "--name-only", "--diff-filter=ACMR"))
                .directory(project.rootDir)
                .redirectErrorStream(true)
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            return output
                .lineSequence()
                .map { it.trim() }
                .filter { it.endsWith(".kt") || it.endsWith(".kts") }
                .map { project.rootDir.resolve(it).absolutePath }
                .toList()
        }

        val inputs = stagedKotlinFiles()
        if (inputs.isEmpty()) {
            println("No staged Kotlin files detected; skipping detektChanged.")
            // Provide a harmless invocation so the JavaExec task completes successfully
            args = listOf("--help")
            return@doFirst
        }

        val reportsDir = layout.buildDirectory.dir("reports/detekt").get().asFile
        reportsDir.mkdirs()

        val arguments = mutableListOf(
            "--input", inputs.joinToString(","),
            "--config", project.file("config/detekt/detekt.yml").absolutePath,
            "--build-upon-default-config",
            "--parallel",
            "--excludes",
            "**/build/**,**/build/generated/**," +
                "**/build/generated-sources/**,**/buildSrc/build/generated-sources/**",
            "--report", "txt:${reportsDir.resolve("detekt-changed.txt").absolutePath}",
            "--report", "sarif:${reportsDir.resolve("detekt-changed.sarif").absolutePath}",
        )
        val baselineFile = project.file("config/detekt/baseline.xml")
        if (baselineFile.exists()) {
            arguments += listOf("--baseline", baselineFile.absolutePath)
        }
        val pluginClasspath = detektPlugins.resolve()
        if (pluginClasspath.isNotEmpty()) {
            arguments += listOf("--plugins", pluginClasspath.joinToString(",") { it.absolutePath })
        }
        args = arguments
    }
}

// Unified entrypoint to mirror the pre-commit gate (without auto-formatting)
tasks.register("preCommitCheck") {
    group = "verification"
    description = "Runs ktlint checks across subprojects and detektChanged on staged files."
    // run ktlintCheck in all subprojects
    dependsOn(subprojects.map { "${'$'}{it.path}:ktlintCheck" })
    // fast detekt over staged files
    dependsOn("detektChanged")
}

// Register the installGitHooks task
tasks.register("installGitHooks") {
    group = "build setup"
    description = "Install Git hooks for pre-commit and pre-push checks"

    doLast {
        val gitHooksDir = File(project.rootDir, ".git/hooks")
        if (!gitHooksDir.exists()) {
            logger.warn("Git hooks directory not found. Make sure you're in a Git repository.")
            return@doLast
        }

        // Create pre-commit hook
        val preCommitHook = File(gitHooksDir, "pre-commit")
        preCommitHook.writeText(
            """
            #!/bin/sh
            # Pre-commit hook to auto-format and run ktlint + detekt on staged Kotlin files
            
            set -e
            echo "Running pre-commit checks..."
            
            # Ensure we run at repo root
            REPO_ROOT="${'$'}(git rev-parse --show-toplevel)"
            cd "${'$'}REPO_ROOT"
            
            # Auto-format Kotlin sources
            ./gradlew --no-daemon --stacktrace ktlintFormat
            
            # Re-stage any formatting changes
            git add -A
            
            # Run checks: ktlint + fast detekt on staged files only
            ./gradlew --no-daemon --stacktrace ktlintCheck detektChanged
            
            # Check the result
            if [ ${'$'}? -ne 0 ]; then
                echo "Pre-commit checks failed. Please fix the issues before committing."
                exit 1
            fi
            
            echo "Pre-commit checks passed."
            exit 0
            """
                .trimIndent()
        )

        // Make pre-commit hook executable
        preCommitHook.setExecutable(true)

        // Create pre-push hook
        val prePushHook = File(gitHooksDir, "pre-push")
        prePushHook.writeText(
            """
            #!/bin/sh
            # Pre-push hook to run comprehensive checks
            
            echo "Running pre-push checks..."
            
            # Run all checks
            ./gradlew --no-daemon --stacktrace clean check detektAll ktlintCheck versionCheck schemaCompatibilityCheck
            
            # Check the result
            if [ ${'$'}? -ne 0 ]; then
                echo "Pre-push checks failed. Please fix the issues before pushing."
                exit 1
            fi
            
            echo "Pre-push checks passed."
            exit 0
            """
                .trimIndent()
        )

        // Make pre-push hook executable
        prePushHook.setExecutable(true)

        logger.lifecycle("Git hooks installed successfully!")
        logger.lifecycle("Pre-commit hook: Runs ktlintFormat, ktlintCheck, detektChanged (staged files)")
        logger.lifecycle("Pre-push hook: Runs clean check detektAll ktlintCheck versionCheck schemaCompatibilityCheck")
    }
}

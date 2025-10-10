plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.danilopianini:gradle-pre-commit-git-hooks:2.0.10")
}
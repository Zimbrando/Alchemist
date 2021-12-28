plugins {
    `kotlin-dsl`
    id("org.danilopianini.gradle-kotlin-qa") version "0.8.3"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        allWarningsAsErrors = false
    }
}

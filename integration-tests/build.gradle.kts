plugins {
    java
    alias(libs.plugins.spring.dependency.management)
}

group = "ru.polyshelf"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.jayway.jsonpath:json-path")
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    enabled = false
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Full stack integration tests: gateway, auth, core, datamatrix-adapter, postgres in Docker"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    dependsOn(":core:bootJar", ":auth:bootJar", ":gateway:bootJar", ":datamatrix-adapter:bootJar")
    systemProperty("repo.root", rootProject.projectDir.absolutePath)
    outputs.upToDateWhen { false }
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

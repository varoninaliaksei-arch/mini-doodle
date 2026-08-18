plugins {
    id("java-library")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

dependencies {
    implementation(project(":application"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.spring.boot.flyway)
    runtimeOnly(libs.postgresql)

    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.tracing.bridge.otel)

    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("com.minidoodle.infrastructure.InfrastructureApplication")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Testcontainers-backed integration tests (real postgres:17, real Spring
// context) are Docker-dependent and slow - kept out of the default `test`/
// `check`/`build` graph so those stay fast and Docker-independent. Run
// explicitly via `./gradlew integrationTest`.
sourceSets {
    create("integrationTest") {
        java.srcDir("src/integrationTest/java")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
}

tasks.register<Test>("integrationTest") {
    description = "Runs Testcontainers-backed integration tests (requires Docker)."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

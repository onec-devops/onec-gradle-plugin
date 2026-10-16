import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.plugin.publish)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    coreLibrariesVersion = "2.2.0"
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = apiVersion
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

gradlePlugin {
    website = "https://github.com/onec-devops/onec-gradle-plugin"
    vcsUrl = "https://github.com/onec-devops/onec-gradle-plugin"
    plugins.create("onec") {
        id = "io.github.onec-devops.onec"
        implementationClass = "io.github.onecdevops.OnecPlugin"
        displayName = "Onec Gradle Plugin"
        description = "Gradle plugin for 1C:Enterprise development. Builds configurations, extensions, external data processors and reports with ibcmd."
        tags = listOf("onec", "1c", "1cv8", "1c-enterprise", "bsl", "ibcmd")

        compatibility {
            features {
                configurationCache = true
            }
        }
    }
}

publishing.publications.create<MavenPublication>("pluginMaven") {
    artifactId = "onec-gradle-plugin"
}

val functionalTestSourceSet = sourceSets.create("functionalTest")

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTest = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    // -PtestGradleVersion=9.0.0 runs the test builds on that Gradle instead of the current one.
    providers.gradleProperty("testGradleVersion").orNull?.let { systemProperty("testGradleVersion", it) }
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.validatePlugins {
    enableStricterValidation = true
}

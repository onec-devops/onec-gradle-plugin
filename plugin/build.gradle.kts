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
        description = "Gradle support for 1C:Enterprise development."
        tags = listOf("1c", "1c-edt", "1c-enterprise", "1cv8", "onec")

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
    // -PtestPlatformVersion=8.3.25.1633 and -PtestEdtVersion=2026.1.3 make the tests ask for that platform
    // and 1C:EDT instead of any build of 8.3 and of 2026.
    providers.gradleProperty("testPlatformVersion").orNull?.let { systemProperty("testPlatformVersion", it) }
    providers.gradleProperty("testEdtVersion").orNull?.let { systemProperty("testEdtVersion", it) }
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

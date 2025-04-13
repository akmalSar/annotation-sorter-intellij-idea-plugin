import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.changelog.Changelog
plugins {
    id("java")
    id("checkstyle")
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    id("io.spring.javaformat") version "0.0.43"
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
}
group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}
// Configure project's dependencies
repositories {
    mavenCentral()


    intellijPlatform {
        defaultRepositories()
    }
}
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:unchecked")
}
tasks.test {
    useJUnitPlatform()
}
tasks.named("checkFormatTest") {
    // Declare ':formatTest' as an input of ':checkFormatTest'
    inputs.files(tasks.named("formatTest"))

    // Declare an explicit dependency on ':formatTest'
    dependsOn(tasks.named("formatTest"))

    // Declare an explicit dependency on ':formatTest' using mustRunAfter
    mustRunAfter(tasks.named("formatTest"))
}
tasks.named("checkFormatMain") {
    // 1. Declare ':formatMain' as an input of ':checkFormatMain'
    inputs.files(tasks.named("formatMain"))

    // 2. Declare an explicit dependency on ':formatMain' from ':checkFormatMain' using Task#dependsOn
    dependsOn(tasks.named("formatMain"))

    // 3. Declare an explicit dependency on ':formatMain' from ':checkFormatMain' using Task#mustRunAfter
    mustRunAfter(tasks.named("formatMain"))
}
checkstyle {
    toolVersion = "10.0"
    config = resources.text.fromUri("/io/spring/javaformat/checkstyle/checkstyle.xml")
}
// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3")
    checkstyle("io.spring.javaformat:spring-javaformat-checkstyle:0.0.43")
    // JUnit Jupiter (JUnit 5)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.27.3")

    // IntelliJ test framework
//    testImplementation(platform("org.jetbrains.intellij.plugins:gradle-intellij-plugin:8.10.2"))
//    testImplementation("com.jetbrains.intellij.idea:ideaIC:2024.3.5")
    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        javaCompiler()
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        pluginVerifier()
        zipSigner()
    }
    tasks.named("compileTestJava") {
        dependsOn(tasks.named("formatTest"))
    }
    tasks.named("compileJava") {
        dependsOn(tasks.named("formatMain"))
    }
    tasks.named("buildPlugin") {
        dependsOn(tasks.test)
        dependsOn(tasks.named("format"))
        dependsOn(tasks.named("check"))
//        dependsOn(tasks.named("verifyPlugin"))
    }
}
// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
// Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }
        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}


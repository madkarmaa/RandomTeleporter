import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom")
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("com.diffplug.spotless") version "8.11.0"
}

val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val loaderVersion = providers.gradleProperty("loader_version").get()
val fabricApiVersion = providers.gradleProperty("fabric_api_version").get()
val fabricKotlinVersion = providers.gradleProperty("fabric_kotlin_version").get()

dependencies {
    // gradle.properties sets the versions.
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")

    // Fabric API, which the mod requires.
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")
}

tasks.processResources {
    val properties =
        mapOf(
            "version" to version,
            "minecraft_version" to minecraftVersion,
            "loader_version" to loaderVersion,
        )
    inputs.properties(properties)

    filesMatching("fabric.mod.json") {
        expand(properties)
    }
}

// The game version is part of the file name, since each build only supports one Minecraft version.
tasks.withType<AbstractArchiveTask>().configureEach {
    archiveVersion = "${project.version}+$minecraftVersion"
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}

java {
    // Loom attaches sourcesJar to a RemapSourcesJar task and to the build task. Without this
    // line, the build produces no sources jar.
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

spotless {
    kotlin {
        target("**/*.kt")
        // Eclipse-based tooling such as jdtls copies the sources into bin.
        targetExclude("**/build/**", "**/bin/**")

        ktfmt().configure {
            it.setMaxWidth(100)
            it.setBlockIndent(4)
            it.setContinuationIndent(4)
        }

        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")

        ktfmt().configure {
            it.setMaxWidth(100)
            it.setBlockIndent(4)
            it.setContinuationIndent(4)
        }

        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)

    from("LICENSE") {
        rename { "${it}_$projectName" }
    }
}

val bun: String =
    providers
        .environmentVariable("PATH")
        .get()
        .split(File.pathSeparator)
        .map { File(it, "bun") }
        .firstOrNull { it.canExecute() }
        ?.path ?: "${System.getProperty("user.home")}/.bun/bin/bun"

val formatWithBun = tasks.register<Exec>("formatWithBun") { commandLine(bun, "fmt") }

tasks.spotlessApply { finalizedBy(formatWithBun) }

val checkFormatWithBun =
    tasks.register<Exec>("checkFormatWithBun") { commandLine(bun, "fmt", "--check") }

tasks.spotlessCheck { finalizedBy(checkFormatWithBun) }

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.nio.file.Paths

plugins {
    id("net.fabricmc.fabric-loom")
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/") }
}

afterEvaluate {
    val MixinJarPath = configurations.loaderLibraries.get().resolvedConfiguration
        .resolvedArtifacts
        .find { it.moduleVersion.id.group == "net.fabricmc" && it.moduleVersion.id.name == "sponge-mixin" }!!
        .file
    val IsSupportDceVM = Paths.get(System.getProperty("java.home"))
        .resolve("bin")
        .resolve("java")
        .toFile()
        .let {
            val ReturnCode = ProcessBuilder(it.absolutePath, "-XX:+AllowEnhancedClassRedefinition", "-version")
                .start()
                .waitFor()
            return@let ReturnCode == 0 }
    loom.runs.named("client") {
        jvmArguments.add("-javaagent:${MixinJarPath.absolutePath}")
        if (IsSupportDceVM == true) jvmArguments.add("-XX:+AllowEnhancedClassRedefinition") }
    loom.runs.named("server") {
        jvmArguments.add("-javaagent:${MixinJarPath.absolutePath}")
        if (IsSupportDceVM == true) jvmArguments.add("-XX:+AllowEnhancedClassRedefinition") } }

loom {
    splitEnvironmentSourceSets()

    mods {
        register("randomteleporter") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")

    implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

    // Fabric API. This is technically optional, but you probably want it anyway.
    implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

    // Uncomment the following line to enable the deprecated Fabric API modules. 
    // These are included in the Fabric API production distribution and allow you to update your mod to the latest modules at a later more convenient time.
    // "modImplementation"("net.fabricmc.fabric-api:fabric-api-deprecated:${project.extra["fabric_version"]}")

    // ↓ 开发测试用
    runtimeOnly("com.terraformersmc:modmenu:${providers.gradleProperty("modmenu_version").get()}")
}


tasks.processResources {
    val version = version
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25 }

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25 } }

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25 }

tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)

    from("LICENSE") {
        rename { "${it}_$projectName" } }

    // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html#org.gradle.api.tasks.bundling.Jar:archiveFileName
    archiveFileName = "RandomTeleporter-${project.version} mc${project.extra["minecraft_version"]}.jar"}

tasks.named<Jar>("sourcesJar") {
    archiveFileName = "RandomTeleporter-${project.version} mc${project.extra["minecraft_version"]}-sources.jar"}

// configure the maven publication
publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
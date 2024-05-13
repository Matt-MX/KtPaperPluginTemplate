import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.ajoberstar.grgit") version "4.1.0"

    // Check for new versions at https://plugins.gradle.org/plugin/io.papermc.paperweight.userdev
    id("io.papermc.paperweight.userdev") version "1.7.1"
    `maven-publish`
}

group = project.properties["group"].toString()
version = project.properties["version"].toString()
val id: String by project

val plugin_name: String by project
val plugin_main_class_name: String by project
val plugin_author: String by project
val include_commit_hash: Boolean by project

val ktgui_version: String by project
val paper_version: String by project

repositories {
    mavenCentral()
    maven("https://maven.pvphub.me")
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly(kotlin("stdlib"))

    compileOnly("com.mattmx:ktgui:${ktgui_version}")
    paperweight.paperDevBundle(paper_version)
}

tasks {
    withType<ProcessResources> {
        val props = mapOf(
            "name" to plugin_name,
            "main" to "${group}.${id}.${plugin_main_class_name}",
            "author" to plugin_author,
            "version" to if (include_commit_hash) "${rootProject.version}-commit-${grgit.head().abbreviatedId}" else rootProject.version.toString()
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        mergeServiceFiles()
    }

    test {
        useJUnitPlatform()
    }

    assemble {
        dependsOn("reobfJar")
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

sourceSets["main"].resources.srcDir("src/resources/")

kotlin {
    jvmToolchain(17)
}

publishing {
    repositories {
        maven {
            name = "pvphub-releases"
            url = uri("https://maven.pvphub.me/releases")
            credentials {
                username = System.getenv("PVPHUB_MAVEN_USERNAME")
                password = System.getenv("PVPHUB_MAVEN_SECRET")
            }
        }
    }
    publications {
        create<MavenPublication>(id) {
            from(components["java"])
            groupId = group
            artifactId = id
            version = rootProject.version.toString()
        }
    }
}
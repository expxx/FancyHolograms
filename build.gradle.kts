import java.io.BufferedReader
import java.io.InputStreamReader

plugins {
    id("java-library")
    id("maven-publish")

    id("xyz.jpenilla.run-paper") version "2.2.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

runPaper.folia.registerTask()

allprojects {
    group = "de.oliver"
    version = "2.0.3"

    description = "Simple, lightweight and fast hologram plugin using display entities"


    repositories {
        mavenCentral()

        maven(url = "https://papermc.io/repo/repository/maven-public/")

        maven(url = "https://repo.fancyplugins.de/snapshots")
        maven(url = "https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven(url = "https://repo.viaversion.com/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${findProperty("minecraftVersion")}-R0.1-SNAPSHOT")

    implementation(project(":api"))
    implementation(project(":implementation_1_20_2", configuration = "reobf"))
    implementation(project(":implementation_1_20_1", configuration = "reobf"))
    implementation(project(":implementation_1_19_4", configuration = "reobf"))

    implementation("de.oliver:FancyLib:${findProperty("fancyLibVersion")}")

    compileOnly("de.oliver:FancyNpcs:${findProperty("fancyNpcsVersion")}")
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveBaseName.set(rootProject.name)
    }

    runServer {
        minecraftVersion(findProperty("minecraftVersion").toString())

        downloadPlugins {
            hangar("FancyNpcs", findProperty("fancyNpcsVersion").toString())
            hangar("PlaceholderAPI", findProperty("placeholderapiVersion").toString())
            modrinth("miniplaceholders", "M6gjRuIx")
        }
    }

    publishing {
        repositories {
            maven {
                name = "fancypluginsReleases"
                url = uri("https://repo.fancyplugins.de/releases")
                credentials(PasswordCredentials::class)
                authentication {
                    isAllowInsecureProtocol = true
                    create<BasicAuthentication>("basic")
                }
            }

            maven {
                name = "fancypluginsSnapshots"
                url = uri("https://repo.fancyplugins.de/snapshots")
                credentials(PasswordCredentials::class)
                authentication {
                    isAllowInsecureProtocol = true
                    create<BasicAuthentication>("basic")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = rootProject.group.toString()
                artifactId = rootProject.name
                version = rootProject.version.toString()
                from(project.components["java"])
            }
        }
    }


    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything

        val props = mapOf(
            "description" to project.description,
            "version" to project.version,
            "hash" to getCurrentCommitHash(),
            "build" to (System.getenv("BUILD_ID") ?: "").ifEmpty { "undefined" }
        )

        inputs.properties(props)

        filesMatching("plugin.yml") {
            expand(props)
        }

        filesMatching("version.yml") {
            expand(props)
        }
    }
}

fun getCurrentCommitHash(): String {
    val process = ProcessBuilder("git", "rev-parse", "HEAD").start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val commitHash = reader.readLine()
    reader.close()
    process.waitFor()
    if (process.exitValue() == 0) {
        return commitHash ?: ""
    } else {
        throw IllegalStateException("Failed to retrieve the commit hash.")
    }
}
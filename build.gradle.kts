
import kr.entree.spigradle.kotlin.jitpack
import kr.entree.spigradle.kotlin.paper
import kr.entree.spigradle.kotlin.vaultAll

plugins {
    kotlin("jvm") version "1.4.21"

    id("kr.entree.spigradle") version "2.2.3"
    id("com.github.johnrengelman.shadow") version "5.2.0"

    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
    id("eclipse")

    id("net.nemerosa.versioning") version "2.14.0"
}

group = "com.dumbdogdiner"
version = "1.0.0-beta"

repositories {
    mavenCentral()
    jitpack()
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven {
        credentials {
            username = "${property("ghUser")}"
            password = "${property("ghPass")}"
        }
        url = uri("https://maven.pkg.github.com/DumbDogDiner/StickyAPI/")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    implementation("org.jetbrains.exposed:exposed-core:0.29.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.29.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.29.1")
    implementation("org.postgresql:postgresql:42.2.16")

    compileOnly(vaultAll())
    compileOnly(paper("1.16.5"))

    compileOnly("me.clip:placeholderapi:2.10.9")

    implementation("de.tr7zw:item-nbt-api-plugin:2.7.1")
    implementation("com.dumbdogdiner:stickyapi:2.1.0")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.2")
}

tasks {
    ktlintKotlinScriptCheck {
        dependsOn("ktlintFormat")
    }

    build {
        dependsOn("shadowJar")
    }

    shadowJar {
        archiveClassifier.set("")
    }

    spigot {
        authors = listOf("spazzylemons")
        softDepends = listOf("AnimatedScoreboard", "PlaceholderAPI", "Vault")
        depends = listOf()

        // Construct a new version string including git info
        version = "${project.version}_${versioning.info.display}"

        apiVersion = "1.16"
        permissions {
            create("stickysurvival.join") {
                description = "Allows a user to join and spectate games."
                defaults = "true"
            }

            create("stickysurvival.leave") {
                description = "Allows a user to leave games. Does not prevent leaving games by disconnecting."
                defaults = "true"
            }

            create("stickysurvival.reload") {
                description = "Allows a user to reload the plugin."
                defaults = "op"
            }

            create("stickysurvival.kit") {
                description = "Allows a user to choose a kit."
                defaults = "true"
            }

            create("stickysurvival.kits") {
                description = "Allows a user to list all kits."
                defaults = "true"
            }

            create("stickysurvival.forcestart") {
                description = "Allows a player to force a game to start if there are not enough players."
                defaults = "op"
            }

            create("stickysurvival.version") {
                description = "Allows a player to check the plugin version."
                defaults = "op"
            }

            create("stickysurvival.setup") {
                description = "Allows a player to use world setup commands."
                defaults = "op"
            }
        }
    }

    eclipse {
        classpath {
            containers = mutableSetOf("org.eclipse.buildship.core.gradleclasspathcontainer")
        }
    }
}

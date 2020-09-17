
import kr.entree.spigradle.kotlin.paper

plugins {
    kotlin("jvm") version "1.3.72"

    id("kr.entree.spigradle") version "2.0.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"

    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("eclipse")
}

group = "com.dumbdogdiner"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    compileOnly(paper("1.16.1"))

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
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
        authors = listOf("You")
        softDepends = listOf()
        apiVersion = "1.16"
        commands {}
    }

    eclipse {
        classpath {
            containers = mutableSetOf("org.eclipse.buildship.core.gradleclasspathcontainer")
        }
    }
}

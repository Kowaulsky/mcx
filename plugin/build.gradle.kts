plugins {
    kotlin("jvm") version "2.0.0"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "me.kowaulsky"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.mindrot:jbcrypt:0.4")
    // Jackson dependencies for JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2") // This provides jacksonObjectMapper()
}

tasks {
    runServer {
        minecraftVersion("1.20.6")
    }

    shadowJar {
        // Relocation rules to avoid conflicts in the plugin environment
        relocate("okhttp3", "me.kowaulsky.auditium.okhttp3")
        relocate("kotlinx.serialization", "me.kowaulsky.auditium.kotlinx.serialization") // More specific relocation
        relocate("com.zaxxer.hikari", "me.kowaulsky.auditium.hikari")
        relocate("com.mysql", "me.kowaulsky.auditium.sql")
        relocate("org.slf4j", "me.kowaulsky.auditium.slf4j") // Corrected typo here
        relocate("com.fasterxml.jackson", "me.kowaulsky.auditium.jackson")
        relocate("org.mindrot", "me.kowaulsky.auditium.jbcrypt")
        archiveClassifier.set("") // Ensures the shadow JAR is the primary output
    }
}

// Target Java version for Kotlin compilation
val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

// Make shadowJar the primary build artifact
tasks.build {
    dependsOn("shadowJar")
}

// Process plugin.yml to inject version from build.gradle.kts
tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
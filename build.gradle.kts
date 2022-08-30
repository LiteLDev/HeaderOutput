import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    java
    distribution
    application
}

group = "com.liteldev"
version = "1.0-SNAPSHOT"
val mainName = "com.liteldev.headeroutput.HeaderOutput"

application {
    mainClassName = mainName
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // https://mvnrepository.com/artifact/commons-cli/commons-cli
    implementation("commons-cli:commons-cli:1.5.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf("Main-Class" to mainName)
        )
    }
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(
            mapOf("Main-Class" to mainName)
        )
    }
}
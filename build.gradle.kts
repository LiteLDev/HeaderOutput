import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    java
    distribution
    application
}

group = "com.liteldev"
version = "1.0-SNAPSHOT"
val mainName = "com.liteldev.headeroutput.HeaderOutput"

application {
    mainClass.set(mainName)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")

    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.getByName<ShadowJar>("shadowJar") {
    minimize()
}

tasks.build { dependsOn(tasks.named("shadowJar")) }

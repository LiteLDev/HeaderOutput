plugins {
    kotlin("jvm") version "1.5.10"
    java
}

group = "tech.rimuruchan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.alibaba:fastjson:1.2.78")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.10")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
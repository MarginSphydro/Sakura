plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.5"
    application
}

group = "dev.sakura"
version = "1.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.aliyun.com/repository/central")
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.ow2.asm:asm:9.7.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("dev.sakura.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("AutoCreate")
    archiveClassifier.set("")
    archiveVersion.set("")
}

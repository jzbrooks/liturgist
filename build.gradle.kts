plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.jzbrooks"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10")
    implementation("com.openhtmltopdf:openhtmltopdf-svg-support:1.0.10")
    implementation("org.jetbrains.kotlinx:dataframe:0.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.github.jknack:handlebars:4.3.1")
    implementation("com.squareup.okio:okio:3.6.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.jzbrooks.MainKt"
    }
}

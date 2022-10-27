plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    `java-gradle-plugin`
    `maven-publish`
}

group = "de.gematik.kether-plugin"
version = "1.0-SNAPSHOT"

gradlePlugin {
    plugins {
        create("ketherCodeGenerator") {
            id = "de.gematik.kether.codegen"
            implementationClass = "de.gematik.kether.codegen.CodeGeneratorPlugin"
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}
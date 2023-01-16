plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    id("maven-publish")
    id("java-gradle-plugin")
}

group = "de.gematik.kether"
version = "1.0"

gradlePlugin {
    plugins {
        create("ketherCodeGenerator") {
            id = "de.gematik.kether.codegen"
            implementationClass = "de.gematik.kether.codegen.CodeGeneratorPlugin"
        }
    }
}

repositories {
    maven(url="https://repo.labor.gematik.de/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("de.gematik.kether:solckt:0.0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    `java-gradle-plugin`
    `maven-publish`
}

group = "de.gematik.kether-plugin"
version = "1.2.2"

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
    implementation("de.gematik.kether:solckt:1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}
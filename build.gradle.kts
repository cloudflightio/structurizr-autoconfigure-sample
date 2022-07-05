plugins {
    id("io.cloudflight.autoconfigure-gradle") version "0.5.3"
}

description = "Sample application for the Spring Boot AutoConfigure Support for the Structurizr Client"
group = "io.cloudflight.structurizr.sample"
version = "1.0.0"

autoConfigure {
    java {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.cloudflight.structurizr:structurizr-autoconfigure:1.0.1")
}
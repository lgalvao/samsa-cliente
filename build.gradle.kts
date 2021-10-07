plugins {
    java
    application
}

group = "sesel"
version = "0.8.2"
description = "Samsa Cliente"

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

configurations{
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.4")
    compileOnly ("org.projectlombok:lombok:1.18.20")
    annotationProcessor ("org.projectlombok:lombok:1.18.20")
}

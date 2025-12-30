plugins {
    application
    id("org.beryx.runtime") version "1.13.0" // JVM同梱用
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":nextJFX-core"))
}

application {
    mainClass.set("net.murasakiyamaimo.Main")
}
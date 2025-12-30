plugins {
    `java-library`
    id("com.google.osdetector")
}

val lwjglVersion = "3.3.6"

dependencies {
    // APIとして公開するもの（appモジュールからも見える）
    api("org.jdom:jdom2:2.0.6.1")
    api("com.fasterxml.jackson.core:jackson-databind:2.19.2")

    // LWJGL 共通
    val lwjglModules = listOf("", "-nanovg", "-assimp", "-glfw", "-jemalloc", "-openal", "-opengl", "-stb", "-freetype")
    lwjglModules.forEach {
        implementation("org.lwjgl:lwjgl$it:$lwjglVersion")
        runtimeOnly("org.lwjgl:lwjgl$it:$lwjglVersion:natives-windows")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // モダンJava 21推奨
    }
}
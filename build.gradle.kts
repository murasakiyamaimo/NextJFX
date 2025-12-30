allprojects {
    repositories {
        mavenCentral() // これで JDOM2 や Jackson が見つかるようになります
    }
}

plugins {
    id("com.google.osdetector") version "1.7.3" apply false
}

dependencies {
    // osdetector.classifier が "windows-x86_64" や "osx-aarch_64" を自動生成
    runtimeOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:${osdetector.classifier}")
}
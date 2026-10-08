allprojects {
    repositories {
        mavenCentral() // これで JDOM2 や Jackson が見つかるようになります
    }
}

plugins {
    `java`
}
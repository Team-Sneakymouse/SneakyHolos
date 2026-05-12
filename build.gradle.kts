plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly(files("../run/versions/1.21.4/paper-1.21.4.jar"))
    compileOnly("io.netty:netty-all:4.1.112.Final")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
    implementation("com.google.code.gson:gson:2.10.1")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "21"
        }
    }
}

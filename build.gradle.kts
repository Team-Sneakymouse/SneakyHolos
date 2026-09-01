import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
}

repositories {
    mavenCentral()
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    paperweight.paperDevBundle("26.2.build.+")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation(kotlin("stdlib"))
    implementation("com.google.code.gson:gson:2.10.1")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

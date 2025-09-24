import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

android {
    namespace = "io.github.canvas"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.canvas"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.1-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        proguardFiles()
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

dependencies {
    runtimeOnly(project(":ui:launcher"))
    runtimeOnly(project(":ui:settings"))
}

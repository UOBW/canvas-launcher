import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

android {
    namespace = "io.github.canvas.ui.settings"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.iconsextended)
//    implementation(libs.accompanist.permissions)
    implementation(libs.compose.navigation)
    implementation(libs.kotlin.serialization.json)
    implementation(project(":data"))
    implementation(project(":ui:common"))

    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.junit.android)
    androidTestImplementation(libs.test.espresso.core)
    debugImplementation(libs.debug.compose.tooling)
}

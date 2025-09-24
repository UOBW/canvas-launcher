import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

android {
    namespace = "io.github.canvas.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
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

    room {
        schemaDirectory("room_schemas")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    // Proto datastore
    implementation(libs.androidx.core.ktx)
    implementation(libs.datastore.proto)
    implementation(project(":datastore-proto"))
    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    // Some icons need to be stored in the data layer
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    // Allow ActivityResultContracts to be defined in the data layer
    implementation(libs.androidx.activity.ktx)

    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.junit.android)
    androidTestImplementation(libs.test.espresso.core)
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

android {
    namespace = "io.github.canvas.ui.launcher"
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
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        // Shouldn't be required anymore, but Android Studio complains otherwise
        freeCompilerArgs.add("-XXLanguage:+WhenGuards")
    }
}

// Uncomment to enable compose reports generation
//composeCompiler {
//    metricsDestination.set(project.layout.buildDirectory.dir("compose_metrics"))
//    reportsDestination.set(project.layout.buildDirectory.dir("compose_metrics"))
//}

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
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.compose.reorderable)
    implementation(project(":data"))
    implementation(project(":ui:common"))

    debugImplementation(libs.debug.compose.tooling)
    debugImplementation(libs.debug.compose.manifest)

    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.junit.android)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.test.compose.junit)
}

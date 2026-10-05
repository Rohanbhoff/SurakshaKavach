/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  YOLOv8s-Seg Android App — Module-level Build Configuration
 *  CameraX + TensorFlow Lite + NNAPI/GPU Hardware Acceleration
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Place this file at: YourAndroidProject/app/build.gradle.kts
 *  Then copy best-int8.tflite → app/src/main/assets/best-int8.tflite
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}


android {
    namespace = "com.yoloseg.edgeai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yoloseg.edgeai"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        viewBinding = true
        compose = true
    }



    // ─── CRITICAL: Prevent compression of TFLite model assets ───
    // Without this, the TFLite interpreter cannot memory-map the model file,
    // resulting in a significant performance penalty or crash on load.
    androidResources {
        noCompress += listOf("tflite")
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Dependency Versions — Centralized for maintainability
// ═══════════════════════════════════════════════════════════════════════════

val cameraxVersion = "1.4.2"
val tfliteVersion = "2.16.1"
val lifecycleVersion = "2.8.7"
val coroutinesVersion = "1.9.0"

dependencies {

    // ─── Core Android ───
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.activity:activity-compose:1.9.3")

    // ─── Compose ───
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.compose.runtime:runtime-livedata")

    // ─── Lifecycle (ViewModel + Runtime) ───
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")

    // ─── Kotlin Coroutines ───
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    // ═══════════════════════════════════════════════════════════════════════
    //  CameraX — Camera System Modules
    //  Provides the camera preview, frame analysis, and lifecycle bindings
    // ═══════════════════════════════════════════════════════════════════════

    // Core CameraX library — base abstractions for camera operations
    implementation("androidx.camera:camera-core:$cameraxVersion")

    // Camera2 implementation — the actual Camera2 API backend for CameraX
    implementation("androidx.camera:camera-camera2:$cameraxVersion")

    // Lifecycle bindings — automatically ties camera lifecycle to Activity/Fragment
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")

    // CameraX View — PreviewView widget for rendering the camera feed in XML layouts
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ═══════════════════════════════════════════════════════════════════════
    //  TensorFlow Lite — On-Device ML Inference Engine
    //  Runs the INT8-quantized YOLOv8s-seg model locally on the device
    // ═══════════════════════════════════════════════════════════════════════

    // Core TFLite runtime — the interpreter that executes .tflite model graphs
    implementation("org.tensorflow:tensorflow-lite:$tfliteVersion")

    // GPU Delegate — offloads compatible operations to the device GPU via OpenGL/OpenCL
    // Provides 2-7x speedup on supported operations
    implementation("org.tensorflow:tensorflow-lite-gpu:$tfliteVersion")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:$tfliteVersion")

    // ─── Testing ───
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
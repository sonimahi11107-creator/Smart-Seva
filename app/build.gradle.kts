plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.smartseva"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartseva"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

        // CameraX
        implementation("androidx.camera:camera-core:1.3.4")
        implementation("androidx.camera:camera-camera2:1.3.4")
        implementation("androidx.camera:camera-lifecycle:1.3.4")
        implementation("androidx.camera:camera-view:1.3.4")

        // ML Kit
        implementation("com.google.mlkit:text-recognition:16.0.1")
        implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")

    // Firebase (single BOM — no duplicates)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.guava:guava:32.1.3-android")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
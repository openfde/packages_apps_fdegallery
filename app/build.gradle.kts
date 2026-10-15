plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.fde.gallery3d"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.fde.gallery3d.debug"
        minSdk =
            26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // 需要支持的 ABI（含 arm64-v8a）
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
    androidResources {
        noCompress += listOf("tflite")
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}



dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    api(project(":imageeditlibrary"))
    implementation(files("libs/baselib.aar"))
    implementation("com.github.bumptech.glide:glide:5.0.4")
    implementation("com.github.wseemann:FFmpegMediaMetadataRetriever:1.0.14")
    // 离线 OCR：Tesseract4Android（Tesseract 4 LSTM）本地 AAR，
    // 替换在目标设备上无法识别（blockCount=0）的 ML Kit gOCR。
    // 注意：不再保留任何 com.google.mlkit:text-recognition* 依赖——
    // 它们复用同一条 gOCR 原生管线（libmlkit_google_ocr_pipeline.so），
    // 在本设备（ARM64 仿真谎报 dotprod/fp16）上同样返回 0 个文本块。
    implementation(files("libs/tesseract4android-4.8.0.aar"))
    implementation(files("libs/exoplayer-core-2.10.1.aar"))
    implementation(files("libs/exoplayer-ui-2.10.1.aar"))
    implementation(files("libs/exoplayer-r1.5.16.aar"))
    implementation(files("libs/tensorflow-lite-2.16.1.aar"))
    implementation(files("libs/tensorflow-lite-api-2.16.1.aar"))
    implementation(files("../imageeditlibrary/libs/PhotoView-2.3.0.aar"))

    implementation("com.google.firebase:firebase-functions:22.1.0")
    implementation("com.google.code.gson:gson:2.8.6")
}


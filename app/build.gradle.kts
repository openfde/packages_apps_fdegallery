plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.fde.gallery3d"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.fde.gallery3d"
        minSdk =
            26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

//    compileOnly(files("libs/exoplayer-core-2.10.1.aar"))
//    compileOnly(files("libs/exoplayer-r1.5.16.aar"))
//    compileOnly(files("libs/exoplayer-ui-2.10.1.aar"))
    api(project(":imageeditlibrary"))
    implementation(files("libs/baselib.aar"))
    implementation("com.github.bumptech.glide:glide:5.0.4")
    implementation("com.github.wseemann:FFmpegMediaMetadataRetriever:1.0.14")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    implementation(files("libs/exoplayer-core-2.10.1.aar"))
    implementation(files("libs/exoplayer-ui-2.10.1.aar"))
    implementation(files("libs/exoplayer-r1.5.16.aar"))
}
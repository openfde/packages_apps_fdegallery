// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}


extra["sdkVersion"] = 37
extra["minSdkVersion"] = 26
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
}

val buildTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
val buildTimeFormatted: String = run {
    buildTimeFormatter.timeZone = TimeZone.getTimeZone("UTC")
    buildTimeFormatter.format(Date())
}

android {
    namespace = "org.andrewgao.sony.aspectratio"
    compileSdk {
        // The framework calls Jelly Bean camera APIs. minSdk stays 10 so this still installs on the
        // camera emulator.
        version = release(16)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "org.andrewgao.sony.aspectratio"
        minSdk = 10
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 10
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "BUILD_TIME", "\"$buildTimeFormatted\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.openmemories.framework)
    testImplementation(libs.junit)
}

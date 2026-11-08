plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.andrewgao.sony.aspectratio"
    compileSdk {
        version = release(10)
    }

    defaultConfig {
        applicationId = "org.andrewgao.sony.aspectratio"
        minSdk = 10
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 10
        versionCode = 1
        versionName = "1.0"

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

dependencies {
    testImplementation(libs.junit)
}
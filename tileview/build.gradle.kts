plugins {
    id("com.android.library")
}

android {
    compileSdk = 37
    defaultConfig {
        minSdk = 16
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    namespace = "com.qozix.tileview"
    lint {
        targetSdk = 29
    }
    testOptions {
        targetSdk = 29
    }
}

dependencies {
    implementation(fileTree("libs") { include(listOf("*.jar")) })
    implementation(libs.annotation)
    implementation(libs.material)
    testImplementation(libs.junit)
}

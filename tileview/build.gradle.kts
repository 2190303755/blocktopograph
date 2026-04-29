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
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("com.google.android.material:material:1.13.0")
    testImplementation("junit:junit:4.13.2")
}

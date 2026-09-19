plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    defaultConfig {
        minSdk = 26
        compileSdk = 37
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    namespace = "ovh.plrapps.mapcompose"
    lint {
        targetSdk = 33
    }
    testOptions {
        targetSdk = 33
    }
}
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-Xopt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
                "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi"
            )
        )
    }
}
dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.unit)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.compose.ui.test.junit4)
}

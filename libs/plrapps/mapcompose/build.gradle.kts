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
    api(platform("androidx.compose:compose-bom:2026.05.01"))
    api("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.ui:ui-unit")
    /*implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:${coroutine_version}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${coroutine_version}")*/
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

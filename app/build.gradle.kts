plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    defaultConfig {
        applicationId = "rbq2012.blocktopograph.compat"
        minSdk = 26
        targetSdk = 33
        compileSdk = 37
        versionCode = 1090008
        versionName = "1.9.5"
        vectorDrawables.useSupportLibrary = true
    }
    configurations {
        implementation {
            exclude(group = "org.jetbrains", module = "annotations")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") // TODO configure signing
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        dataBinding = true
        buildConfig = true
        viewBinding = true
        aidl = true
        compose = true
    }
    lint {
        disable += "ExtraTranslation"
    }
    namespace = "com.mithrilmania.blocktopograph"
}

dependencies {
    val composeBoM = "2026.05.00"
    implementation(platform("androidx.compose:compose-bom:$composeBoM"))
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.material3:material3:1.5.0-alpha18")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    testImplementation("junit:junit:4.13.2")
    implementation(project(":tileview"))
    implementation(project(":libs:hivemc:leveldb"))
    implementation("com.github.clans:fab:1.6.4")
    implementation("com.github.woxthebox:draglistview:1.7.2")
    implementation("com.andreabaccega:android-edittext-validator:1.3.5")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.github.bumptech.glide:glide:5.0.7")
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBoM"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.7")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.github.florent37:expansionpanel:1.2.4")
    implementation("com.google.guava:guava:33.6.0-android")
    implementation("com.hivemc.leveldb:leveldb-api:1.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-ktx:1.12.4")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.window:window:1.5.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

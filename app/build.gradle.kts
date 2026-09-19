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
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.material3)
    implementation(libs.material.icons)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    implementation(project(":tileview"))
    implementation(project(":libs:hivemc:leveldb"))
    implementation(project(":libs:plrapps:mapcompose"))
    implementation(libs.clans.fab)
    implementation(libs.annotation)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.coil)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.photoview)
    implementation(libs.expansionpanel)
    implementation(libs.guava)
    implementation(libs.leveldb.api)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.documentfile)
    implementation(libs.core.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.drawerlayout)
    implementation(libs.window)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.reorderable)
    implementation(libs.composeunstyled.bottomsheet)
    implementation(libs.fastutil)
    implementation(libs.paging.common)
    implementation(libs.paging.compose)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)
}

plugins {
    id("com.android.library")
}

android {
    defaultConfig {
        minSdk = 26
        compileSdk = 37
    }
    namespace = "com.hivemc.leveldb"
    lint {
        targetSdk = 33
    }
    testOptions {
        targetSdk = 33
    }
}

dependencies {
    implementation(libs.snappy)
    implementation(libs.guava)
    implementation(libs.leveldb.api)
}

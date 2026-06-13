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
    implementation("org.iq80.snappy:snappy:0.5")
    implementation("com.google.guava:guava:33.6.0-android")
    implementation("com.hivemc.leveldb:leveldb-api:1.1.0")
}

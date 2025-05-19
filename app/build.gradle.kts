plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.paydaylay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.paydaylay"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.google.firebase.appcheck.debug)
    implementation(libs.google.firebase.appcheck.playintegrity)
    // Room dependencies
    val roomVersion = "2.5.2"
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Existing dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.google.material)
    implementation(libs.mpandroidchart)
    implementation(libs.itextpdf)
    implementation(libs.google.services)
    implementation(libs.firebase.core)
    implementation(libs.firebase.storage)
    implementation(libs.opencsv)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.play.services.safetynet)
    implementation (libs.firebase.appcheck)
    implementation (libs.firebase.appcheck.playintegrity)
    implementation (libs.firebase.appcheck.debug)
    implementation(libs.swiperefreshlayout)
    implementation("com.google.code.gson:gson:2.10.1")
}
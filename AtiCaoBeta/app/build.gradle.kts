plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.fivenightsatajisland.aticaobeta"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fivenightsatajisland.aticaobeta"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        viewBinding = true
        mlModelBinding = true
    }

    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation(libs.litert)
    implementation(libs.litert.support) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.play.services.code.scanner)
    implementation(libs.mpandroidchart)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
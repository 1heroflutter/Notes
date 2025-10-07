plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.mynotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mynotes"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation ("androidx.navigation:navigation-compose:2.6.0")
    implementation ("com.airbnb.android:lottie-compose:6.0.0")

    //Room
    implementation("androidx.room:room-runtime:${rootProject.extra["room_version"]}")
    ksp("androidx.room:room-compiler:${rootProject.extra["room_version"]}")
    implementation ("androidx.room:room-ktx:2.5.2")

    //work manager
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.biometric:biometric:1.1.0")

    implementation (libs.androidx.activity.compose.v182)
    implementation (libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.firebase.bom))
    implementation (libs.google.firebase.auth.ktx)
    implementation (libs.play.services.auth)
    implementation (libs.firebase.firestore.ktx)

    //word
    // POI cho file Word
    implementation ("org.apache.poi:poi:5.2.3")
    implementation ("org.apache.poi:poi-ooxml:5.2.3")

    // Để hỗ trợ định dạng Office Open XML (Word .docx)
    implementation ("org.apache.xmlbeans:xmlbeans:5.1.1")

    // Xử lý zip (file Word là dạng zip)
    implementation ("org.apache.commons:commons-compress:1.21")

    //Pdf
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation(libs.facebook.android.sdk)

    //Json
    implementation("com.google.code.gson:gson:2.11.0")

}
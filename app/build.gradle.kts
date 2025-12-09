plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    alias(libs.plugins.navigation.safeargs.kotlin)
}

android {
    namespace = "nl.fijnedagvan.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "nl.fijnedagvan.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }
    buildTypes {
        release {
            isMinifyEnabled = false // Deze regel is vaak standaard, dus kun je laten staan
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Vertelt Gradle om de API_KEY variabele aan te maken voor je release-versie
            buildConfigField("String", "API_KEY", "\"${properties["API_KEY"]}\"")
        }
        debug {
            // Vertelt Gradle om de API_KEY variabele aan te maken voor je debug-versie
            buildConfigField("String", "API_KEY", "\"${properties["API_KEY"]}\"")
        }
    }





    buildFeatures {
        viewBinding = true
        android.buildFeatures.buildConfig = true
    }



    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // --- Standaard Dependencies ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // --- Jouw Toegevoegde Dependencies ---
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("io.coil-kt:coil:2.6.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.window:window:1.3.0")

    // --- Agenda ---
    implementation("com.kizitonwose.calendar:view:2.5.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
}
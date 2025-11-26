plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.sttherese"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sttherese"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

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
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation ("com.google.firebase:firebase-auth")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation ("com.google.firebase:firebase-firestore")
    implementation ("com.google.firebase:firebase-database")
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1")
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.github.f0ris.sweetalert:library:1.5.6")
    implementation("androidx.core:core-splashscreen:1.0.1")
// Or the latest version
// For control over item selection of both touch and mouse driven selection
    implementation("androidx.recyclerview:recyclerview-selection:1.2.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.recyclerview)
    implementation(libs.firebase.common)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.volley)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
//    implementation("com.applandeo:material-calendar-view:1.9.2")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
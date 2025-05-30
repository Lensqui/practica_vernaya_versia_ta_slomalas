import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties().apply {
            load(project.rootProject.file("local.properties").inputStream())
        }
        buildConfigField("String", "SUPABASE_API_KEY", "\"${localProperties.getProperty("supabase.api.key")}\"")
        buildConfigField("String", "SUPABASE_AUTH_TOKEN", "\"${localProperties.getProperty("supabase.auth.token")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"https://ugxudaqslvrvtkwjvzbg.supabase.co\"")
        buildConfigField("String", "MAPKIT_API_KEY", "\"${localProperties.getProperty("MAPKIT_API_KEY")}\"")


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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("org.json:json:20231013")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("org.slf4j:slf4j-simple:2.0.13")
    implementation ("org.osmdroid:osmdroid-android:6.1.18")
    implementation ("com.google.zxing:core:3.5.3")
}


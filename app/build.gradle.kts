plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.oorbitt.launcher"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.oorbitt.launcher"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeCompiler {
        stabilityConfigurationFile = project.layout.projectDirectory.file("compose-stability.conf")
    }
}

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-data"))
    implementation(project(":core-ui"))
    implementation(project(":core-platform"))
    implementation(project(":core-security"))
    implementation(project(":feature-home"))
    implementation(project(":feature-drawer"))
    implementation(project(":feature-search"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-gesture"))
    implementation(project(":feature-iconpack"))
    implementation(project(":feature-theme"))
    implementation(project(":feature-backup"))
    implementation(project(":feature-shizuku"))
    implementation(project(":feature-premium"))
    implementation(project(":feature-applock"))
    implementation(project(":feature-profiles"))
    implementation(project(":feature-vault"))
    implementation(project(":feature-wellness"))
    implementation(project(":feature-orbspace"))
    implementation(project(":feature-onboarding"))
    implementation(project(":feature-stylehub"))



    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    testImplementation(libs.junit)
}

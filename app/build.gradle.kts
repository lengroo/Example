plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.libxposedexample"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.example.libxposedexample"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.1.6"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Keep the modern libxposed metadata in the APK. Gradle normally
    // merges resources/, but be explicit so META-INF/xposed/* survives.
    packaging {
        resources {
            merges += "META-INF/xposed/*"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // compileOnly: LSPosed / Vector provides these classes at runtime
    // inside the hooked process. Bundling them would cause conflicts.
    compileOnly(libs.libxposed.api)
    // service is packaged: it lets the module app talk to the framework.
    implementation(libs.libxposed.service)
    // UI: Compose + MiUIX. MainActivity is a ComponentActivity that
    // calls setContent { MiuixTheme { Scaffold { ... } } }.
    // MiUIX 0.9.4 is a Compose Multiplatform library; its Android
    // artifact brings in the JetBrains Compose foundation/ui layers,
    // which resolve to the matching androidx.compose artifacts.
    implementation(libs.miuix.ui)
    implementation(libs.activity.compose)
}

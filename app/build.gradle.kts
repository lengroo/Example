plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.libxposedexample"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.example.libxposedexample"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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
}

dependencies {
    // compileOnly: LSPosed / Vector provides these classes at runtime
    // inside the hooked process. Bundling them would cause conflicts.
    compileOnly(libs.libxposed.api)
    // service is packaged: it lets the module app talk to the framework.
    implementation(libs.libxposed.service)
}

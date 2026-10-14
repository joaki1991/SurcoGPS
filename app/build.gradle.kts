plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

import java.util.Properties

android {
    namespace = "es.surcogps.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "es.surcogps.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "1.5.0"
    }

    // Firma release con clave LOCAL (local.properties, nunca commiteada).
    // Sin propiedades → APK sin firmar (solo diagnóstico).
    val firmaProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
    }
    val tieneFirma = listOf("surco.storeFile", "surco.storePassword", "surco.keyAlias", "surco.keyPassword")
        .all { firmaProps.containsKey(it) }
    signingConfigs {
        if (tieneFirma) {
            create("release") {
                storeFile = file(firmaProps["surco.storeFile"] as String)
                storePassword = firmaProps["surco.storePassword"] as String
                keyAlias = firmaProps["surco.keyAlias"] as String
                keyPassword = firmaProps["surco.keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (tieneFirma) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core"))

    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation(project(":core"))
}

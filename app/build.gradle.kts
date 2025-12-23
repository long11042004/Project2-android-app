plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.project2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.project2"
        minSdk = 26
        targetSdk = 35
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

    packagingOptions {
        resources {
            // Dòng này để giải quyết lỗi META-INF/INDEX.LIST của bạn
            excludes += "META-INF/INDEX.LIST"
            // Các dòng dưới đây để phòng ngừa các lỗi tương tự khác từ Netty
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.gridlayout)
    implementation(libs.cardview)

    implementation(libs.hivemq.mqtt.client)

    implementation(libs.mpandroidchart)

    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
}